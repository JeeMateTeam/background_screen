import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

enum SmsStatus { sendSuccess, sendFailed }

// Features which can be used in background/foreground task
class NativeBackgroundFunctions {
  /// Platform channel
  static const _channel = MethodChannel('background_screen');

  NativeBackgroundFunctions._internal();
  static final NativeBackgroundFunctions _instance = NativeBackgroundFunctions._internal();

  /// Singleton instance
  static NativeBackgroundFunctions get instance => _instance;

  /// Log tag
  static const TAG = "BackgroundScreen:";

  static const METHOD_SET_SCREEN_ON = 'screenOn';
  static const METHOD_SET_SCREEN_OFF = 'screenOff';
  static const METHOD_SEND_SMS = 'sendSms';
  static const METHOD_SEND_MMS = 'sendMms';

  /// Allows to turn on/off screen
  Future<bool?> setScreen(bool state) async {
    try {
      debugPrint('$TAG: try to toggle screen');
      return await _channel.invokeMethod(state ? METHOD_SET_SCREEN_OFF : METHOD_SET_SCREEN_ON);
    } on PlatformException catch (e) {
      debugPrint('$TAG${e.toString()}');
      return null;
    }
  }

  /// Allows to turn on screen
  Future<bool?> get screenON async {
    try {
      return await _channel.invokeMethod(METHOD_SET_SCREEN_ON,  false);
    } on PlatformException catch (e) {
      debugPrint('$TAG${e.toString()}');
      return true;
    }
  }

  /// Allows to turn off screen
  Future<bool?> get screenOFF async {
    try {
      return await _channel.invokeMethod(METHOD_SET_SCREEN_OFF);
    } on PlatformException catch (e) {
      debugPrint('$TAG${e.toString()}');
      return true;
    }
  }

  /// Allows to send SMS
  Future<SmsStatus> sendSms({
    required String phoneNumber,
    required String body,
    int? simSlot,
  }) async {
    try {
      String? result = await _channel.invokeMethod(METHOD_SEND_SMS, <String, dynamic>{
        "phone": phoneNumber,
        "msg": body,
        "simSlot": simSlot,
      });
      return result == "Sent" ? SmsStatus.sendSuccess : SmsStatus.sendFailed;
    } on PlatformException catch (e) {
      debugPrint('$TAG${e.toString()}');
      return SmsStatus.sendFailed;
    }
  }

  /// Allows to send MMS
  Future<SmsStatus> sendMms({
    required String phoneNumber,
    required String body,
    String? contentUri,
    int? simSlot,
  }) async {
    try {
      String? result = await _channel.invokeMethod(METHOD_SEND_MMS, <String, dynamic>{
        "phone": phoneNumber,
        "msg": body,
        "contentUri": contentUri,
        "simSlot": simSlot,
      });
      return result == "Sent" ? SmsStatus.sendSuccess : SmsStatus.sendFailed;
    } on PlatformException catch (e) {
      debugPrint('$TAG${e.toString()}');
      return SmsStatus.sendFailed;
    }
  }
}