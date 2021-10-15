package io.deepmedia.tools.testing.sample

class AndroidNativeArm64Ops {
    fun usePlatformApi() {
        platform.android.__android_log_write(
            platform.android.ANDROID_LOG_WARN.toInt(),
            "LogTag",
            "LogText!")
    }
}