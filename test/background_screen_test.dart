import 'package:flutter_test/flutter_test.dart';
import 'package:background_screen/background_screen.dart';
import 'package:background_screen/background_screen_platform_interface.dart';
import 'package:background_screen/background_screen_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockBackgroundScreenPlatform
    with MockPlatformInterfaceMixin
    implements BackgroundScreenPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final BackgroundScreenPlatform initialPlatform = BackgroundScreenPlatform.instance;

  test('$MethodChannelBackgroundScreen is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelBackgroundScreen>());
  });

  test('getPlatformVersion', () async {
    BackgroundScreen backgroundScreenPlugin = BackgroundScreen();
    MockBackgroundScreenPlatform fakePlatform = MockBackgroundScreenPlatform();
    BackgroundScreenPlatform.instance = fakePlatform;

    expect(await backgroundScreenPlugin.getPlatformVersion(), '42');
  });
}
