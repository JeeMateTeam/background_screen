package com.scalz.background_screen

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL


/** BackgroundScreenPlugin */
class BackgroundScreenPlugin: MethodCallHandler, FlutterPlugin {
    lateinit var channel : MethodChannel
    val tag = "BackgroundSmsPlugin"
    // channel
    private val SCREEN_CHANNEL = "background_screen"

    // methods
    private val METHOD_WAKE_SCREEN = "screenOn"
    private val METHOD_LOCK_SCREEN = "screenOff"
    private val METHOD_SEND_SMS = "sendSms"
    private val METHOD_SEND_MMS = "sendMms"

    private var applicationContext: Context? = null

    private var deviceManger: DevicePolicyManager? = null
    private var compName: ComponentName? = null

    private var _powerManager: PowerManager? = null
    private var _screenOffWakeLock: PowerManager.WakeLock? = null

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        //val taskQueue: BinaryMessenger.TaskQueue = binding.binaryMessenger.makeBackgroundTaskQueue()
        //channel = MethodChannel(binding.binaryMessenger,SCREEN_CHANNEL,StandardMethodCodec.INSTANCE,taskQueue)

        channel = MethodChannel(binding.binaryMessenger, SCREEN_CHANNEL)
        channel.setMethodCallHandler(this)

        Log.v(tag, "bind backgroundscreen")
        applicationContext = binding.applicationContext
        compName = ComponentName(applicationContext!!, DeviceAdmin::class.java)
        deviceManger = applicationContext!!.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        applicationContext = null
        channel.setMethodCallHandler(null)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        Log.v(tag, "onMethodCall backgroundscreen  ")
        when (call.method) {
            METHOD_WAKE_SCREEN -> {
                Log.v(tag, "onMethodCall backgroundscreen METHOD_WAKE_SCREEN ${deviceManger == null}")
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
                intent.putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "You should enable the app!"
                )

                deviceManger?.lockNow()

                result.success(true)
            }

            METHOD_LOCK_SCREEN -> {
                Log.v(tag, "onMethodCall backgroundscreen METHOD_LOCK_SCREEN")
                enablePhone()
                result.success(true)
            }
            METHOD_SEND_SMS -> {
                Log.v(tag, "onMethodCall backgroundscreen METHOD_SEND_SMS")
                val num: String? = call.argument("phone")
                val msg: String? = call.argument("msg")
                val simSlot: Int? = call.argument("simSlot")
                if (num == null || msg == null) {
                    return result.error("SendSMS failed", "Phone number or message is null", "")
                }
                sendSMS(num, msg, simSlot, result)
            }
            METHOD_SEND_MMS -> {
                Log.v(tag, "onMethodCall backgroundscreen METHOD_SEND_MMS")
                val num: String? = call.argument("phone")
                val msg: String? = call.argument("msg")
                val simSlot: Int? = call.argument("simSlot")
                val url: String? = call.argument("contentUri")
                var contentUri: Uri? = null
                try {
                    contentUri = if (url == null) null else Uri.parse(url)
                }
                catch (ex: java.lang.Exception) {
                    //
                }
                if (num == null || msg == null) {
                    return result.error("SendMMS failed", "Phone number or message is null", "")
                }
                sendMMS(num, msg, url, simSlot, result)
            }
            else -> result.notImplemented()
        }
    }

    private fun enablePhone() {
        _powerManager = applicationContext!!.getSystemService(FlutterFragmentActivity.POWER_SERVICE) as PowerManager
        if (_powerManager != null) {
            _screenOffWakeLock = _powerManager?.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "BackgroundSmsPlugin:onScreen"
            )
            _screenOffWakeLock?.acquire()
        }
    }

    private fun sendSMS(num: String, msg: String, simSlot: Int?, result: MethodChannel.Result) {
        try {
            val smsManager: SmsManager = if (simSlot == null) {
//        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//          applicationContext!!.getSystemService(SmsManager::class.java) as SmsManager
//        } else {
//          SmsManager.getDefault()
//        }
                SmsManager.getDefault()
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    SmsManager.getSmsManagerForSubscriptionId(simSlot)
                } else {
                    SmsManager.getDefault()
                }
            }

            smsManager.sendTextMessage(num, null, msg, null, null)
            result.success("Sent")
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
            result.error("Failed", "Sms Not Sent", "")
        }
    }

    private fun sendMMS(num: String, msg: String, contentUri: String?, simSlot: Int?, result: MethodChannel.Result) {
        try {
            val smsManager: SmsManager = if (simSlot == null) {
//        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//          applicationContext!!.getSystemService(SmsManager::class.java) as SmsManager
//        } else {
//          SmsManager.getDefault()
//        }
                SmsManager.getDefault()
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    SmsManager.getSmsManagerForSubscriptionId(simSlot)
                } else {
                    SmsManager.getDefault()
                }
            }

            val localImageUri:  Uri? = if (contentUri == null)  null else downloadImage(applicationContext!!, contentUri)
            smsManager.sendMultimediaMessage(
                applicationContext,
                localImageUri,
                null, // Optional location URL
                null, // Optional config overrides
                null
            )
            result.success("Sent")
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
            result.error("Failed", "Sms Not Sent", "")
        }
    }

    // Download an image from the internet and save it locally
    private fun downloadImage(context: Context, imageUrl: String): Uri? {
        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        try {
            val url = URL(imageUrl)
            val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val imageFile = File(context.cacheDir, "mms_temp.jpg")
                val inputStream = connection.inputStream
                val outputStream = FileOutputStream(imageFile)

                val buffer = ByteArray(1024)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                outputStream.close()
                inputStream.close()

                return FileProvider.getUriForFile(context, "com.scalz.JeeMate.fileprovider", imageFile)
                //return Uri.fromFile(imageFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }


}

class DeviceAdmin : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "Enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "Disabled", Toast.LENGTH_SHORT).show()
    }
}
