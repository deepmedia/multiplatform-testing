package io.deepmedia.tools.testing.android.tools

/**
 * The [AndroidToolsInspectAllImagesTask] inspect Android system images to check whether the emulator
 * is able to run and what are the abis that it supports. We periodically run the task in CI against
 * all images to get this info, since official support is so poor.
 * See https://issuetracker.google.com/issues/202985152 .
 *
 * The results of these runs is in plain text below, see [MacOsX64Inspections] and [LinuxAmd64Inspections].
 * Luckily the results do not differ that much - abis are the same and only a few emulators run on
 * a machine while hanging on the other. So we're able to define:
 *
 * - a [blocklist] made of the union of the failing emulators in [MacOsX64Inspections] and [LinuxAmd64Inspections]
 * - a [abiLists] map to get the runtime ABIs supported by images that we were able to run
 *   in at least one of the platforms.
 *
 * Note that some of these images might have worked if we waited for longer (inspection timeout is 180s).
 * TODO ARM hosts
 */
internal object ImageInspections {

    fun isInBlocklist(api: Int, abi: String, tag: String): Boolean {
        return "system-images;android-$api;$tag;$abi" in blocklist
    }

    val blocklist: List<String> = listOf(
        "system-images;android-21;default;armeabi-v7a", // macos, linux
        "system-images;android-22;default;armeabi-v7a", // macos, linux
        "system-images;android-24;google_apis;x86_64", // linux
        "system-images;android-24;google_apis_playstore;x86", // linux
        "system-images;android-26;google_apis_playstore;x86", // macos
        "system-images;android-28;google_apis_playstore;x86_64", // macos
        "system-images;android-29;google_apis;arm64-v8a", // macos, linux
        "system-images;android-29;google_apis_playstore;arm64-v8a", // macos, linux
        "system-images;android-30;default;arm64-v8a", // macos, linux
        "system-images;android-30;google_apis;arm64-v8a", // macos, linux
        "system-images;android-30;google_apis_playstore;arm64-v8a", // macos, linux
        "system-images;android-30;google_apis_playstore;x86", // macos
        "system-images;android-30;google_apis_playstore;x86_64", // macos
        "system-images;android-31;default;arm64-v8a", // macos, linux
        "system-images;android-31;google_apis;arm64-v8a", // macos, linux
        "system-images;android-31;google_apis_playstore;arm64-v8a", // macos, linux
        "system-images;android-31;google_apis_playstore;x86_64" // macos
    )

    fun getAbiList(api: Int, abi: String, tag: String): List<String>? {
        return abiLists["system-images;android-$api;$tag;$abi"]
    }

    val abiLists: Map<String, List<String>> = mapOf(
        "system-images;android-21;default;x86_64" to               listOf("x86_64", "x86"),
        "system-images;android-21;google_apis;armeabi-v7a" to      listOf("armeabi-v7a"),
        "system-images;android-21;google_apis;x86_64" to           listOf("x86_64", "x86"),
        "system-images;android-21;default;x86" to                  listOf("x86"),
        "system-images;android-21;google_apis;x86" to              listOf("x86"),
        "system-images;android-22;default;x86_64" to               listOf("x86_64", "x86"),
        "system-images;android-22;google_apis;armeabi-v7a" to      listOf("armeabi-v7a"),
        "system-images;android-22;google_apis;x86_64" to           listOf("x86_64", "x86"),
        "system-images;android-22;default;x86" to                  listOf("x86"),
        "system-images;android-22;google_apis;x86" to              listOf("x86"),
        "system-images;android-23;default;armeabi-v7a" to          listOf("armeabi-v7a"),
        "system-images;android-23;default;x86_64" to               listOf("x86_64", "x86"),
        "system-images;android-23;google_apis;armeabi-v7a" to      listOf("armeabi-v7a"),
        "system-images;android-23;google_apis;x86_64" to           listOf("x86_64", "x86"),
        "system-images;android-23;default;x86" to                  listOf("x86"),
        "system-images;android-23;google_apis;x86" to              listOf("x86"),
        "system-images;android-24;default;armeabi-v7a" to          listOf("armeabi-v7a"),
        "system-images;android-24;default;x86_64" to               listOf("x86_64", "x86"),
        "system-images;android-24;default;arm64-v8a" to            listOf("arm64-v8a", "armeabi-v7a"),
        "system-images;android-24;default;x86" to                  listOf("x86"),
        "system-images;android-24;google_apis;arm64-v8a" to        listOf("arm64-v8a", "armeabi-v7a"),
        "system-images;android-24;google_apis;x86" to              listOf("x86"),
        "system-images;android-24;google_apis;x86_64" to           listOf("x86_64", "x86"),
        "system-images;android-24;google_apis_playstore;x86" to    listOf("x86"),
        "system-images;android-25;default;x86" to                  listOf("x86"),
        "system-images;android-25;default;x86_64" to               listOf("x86_64", "x86"),
        "system-images;android-25;google_apis;arm64-v8a" to        listOf("arm64-v8a", "armeabi-v7a"),
        "system-images;android-25;google_apis;armeabi-v7a" to      listOf("armeabi-v7a"),
        "system-images;android-25;google_apis;x86" to              listOf("x86"),
        "system-images;android-25;google_apis;x86_64" to           listOf("x86_64", "x86"),
        "system-images;android-25;google_apis_playstore;x86" to    listOf("x86"),
        "system-images;android-26;default;x86" to                  listOf("x86"),
        "system-images;android-26;default;x86_64" to               listOf("x86_64", "x86"),
        "system-images;android-26;google_apis;x86" to              listOf("x86"),
        "system-images;android-26;google_apis;x86_64" to           listOf("x86_64", "x86"),
        "system-images;android-26;google_apis_playstore;x86" to    listOf("x86"),
        "system-images;android-27;default;x86" to                  listOf("x86"),
        "system-images;android-27;default;x86_64" to               listOf("x86_64", "x86"),
        "system-images;android-27;google_apis;x86" to              listOf("x86"),
        "system-images;android-27;google_apis_playstore;x86" to    listOf("x86"),
        "system-images;android-28;default;x86" to                  listOf("x86"),
        "system-images;android-28;default;x86_64" to               listOf("x86_64", "x86"),
        "system-images;android-28;google_apis;x86" to              listOf("x86", "armeabi-v7a"),
        "system-images;android-28;google_apis;x86_64" to           listOf("x86_64", "x86"),
        "system-images;android-28;google_apis_playstore;x86" to    listOf("x86", "armeabi-v7a"),
        "system-images;android-28;google_apis_playstore;x86_64" to listOf("x86_64", "x86"),
        "system-images;android-29;default;x86" to                  listOf("x86"),
        "system-images;android-29;default;x86_64" to               listOf("x86_64", "x86"),
        "system-images;android-29;google_apis;x86" to              listOf("x86"),
        "system-images;android-29;google_apis;x86_64" to           listOf("x86_64", "x86"),
        "system-images;android-29;google_apis_playstore;x86" to    listOf("x86"),
        "system-images;android-29;google_apis_playstore;x86_64" to listOf("x86_64", "x86"),
        "system-images;android-30;default;x86_64" to               listOf("x86_64", "x86"),
        "system-images;android-30;google_apis;x86" to              listOf("x86", "armeabi-v7a"),
        "system-images;android-30;google_apis;x86_64" to           listOf("x86_64", "x86", "arm64-v8a", "armeabi-v7a"),
        "system-images;android-30;google_apis_playstore;x86" to    listOf("x86", "armeabi-v7a"),
        "system-images;android-30;google_apis_playstore;x86_64" to listOf("x86_64", "x86", "arm64-v8a", "armeabi-v7a"),
        "system-images;android-31;default;x86_64" to               listOf("x86_64"),
        "system-images;android-31;google_apis;x86_64" to           listOf("x86_64", "arm64-v8a"),
        "system-images;android-31;google_apis_playstore;x86_64" to listOf("x86_64", "arm64-v8a")
    ).mapValues { (image, abis) ->
        val api = image.split(';')[1].split('-')[1].toInt()
        val abi = image.split(';')[3]
        var result = abis
        // Some x86 images before API 30 provided arm execution, maybe early NDK translation
        // that was later refined in API 30. I'm not able to run arm there (undefined symbol: __gnu_Unwind_Find_exidx).
        // Maybe it's a translation issue, maybe it's a Kotlin issue, anyway let's disable this.
        if (abi.startsWith("x86") && api < 30) {
            result = result.filter { !it.startsWith("arm") }
        }
        result
    }
}

private val MacOsX64Inspections = """
INSPECTION RESULTS (70/70)

SUCCESSFUL IMAGES
- system-images;android-21;default;x86_64                   [x86_64, x86]
- system-images;android-21;google_apis;armeabi-v7a          [armeabi-v7a]
- system-images;android-21;google_apis;x86_64               [x86_64, x86]
- system-images;android-21;default;x86                      [x86]
- system-images;android-21;google_apis;x86                  [x86]
- system-images;android-22;default;x86_64                   [x86_64, x86]
- system-images;android-22;google_apis;armeabi-v7a          [armeabi-v7a]
- system-images;android-22;google_apis;x86_64               [x86_64, x86]
- system-images;android-22;default;x86                      [x86]
- system-images;android-22;google_apis;x86                  [x86]
- system-images;android-23;default;armeabi-v7a              [armeabi-v7a]
- system-images;android-23;default;x86_64                   [x86_64, x86]
- system-images;android-23;google_apis;armeabi-v7a          [armeabi-v7a]
- system-images;android-23;google_apis;x86_64               [x86_64, x86]
- system-images;android-23;default;x86                      [x86]
- system-images;android-23;google_apis;x86                  [x86]
- system-images;android-24;default;armeabi-v7a              [armeabi-v7a]
- system-images;android-24;default;x86_64                   [x86_64, x86]
- system-images;android-24;default;arm64-v8a                [arm64-v8a, armeabi-v7a]
- system-images;android-24;default;x86                      [x86]
- system-images;android-24;google_apis;arm64-v8a            [arm64-v8a, armeabi-v7a]
- system-images;android-24;google_apis;x86                  [x86]
- system-images;android-24;google_apis;x86_64               [x86_64, x86]
- system-images;android-24;google_apis_playstore;x86        [x86]
- system-images;android-25;default;x86                      [x86]
- system-images;android-25;default;x86_64                   [x86_64, x86]
- system-images;android-25;google_apis;arm64-v8a            [arm64-v8a, armeabi-v7a]
- system-images;android-25;google_apis;armeabi-v7a          [armeabi-v7a]
- system-images;android-25;google_apis;x86                  [x86]
- system-images;android-25;google_apis;x86_64               [x86_64, x86]
- system-images;android-25;google_apis_playstore;x86        [x86]
- system-images;android-26;default;x86                      [x86]
- system-images;android-26;default;x86_64                   [x86_64, x86]
- system-images;android-26;google_apis;x86                  [x86]
- system-images;android-26;google_apis;x86_64               [x86_64, x86]
- system-images;android-27;default;x86                      [x86]
- system-images;android-27;default;x86_64                   [x86_64, x86]
- system-images;android-27;google_apis;x86                  [x86]
- system-images;android-27;google_apis_playstore;x86        [x86]
- system-images;android-28;default;x86                      [x86]
- system-images;android-28;default;x86_64                   [x86_64, x86]
- system-images;android-28;google_apis;x86                  [x86, armeabi-v7a]
- system-images;android-28;google_apis;x86_64               [x86_64, x86]
- system-images;android-28;google_apis_playstore;x86        [x86, armeabi-v7a]
- system-images;android-29;default;x86                      [x86]
- system-images;android-29;default;x86_64                   [x86_64, x86]
- system-images;android-29;google_apis;x86                  [x86]
- system-images;android-29;google_apis;x86_64               [x86_64, x86]
- system-images;android-29;google_apis_playstore;x86        [x86]
- system-images;android-29;google_apis_playstore;x86_64     [x86_64, x86]
- system-images;android-30;default;x86_64                   [x86_64, x86]
- system-images;android-30;google_apis;x86                  [x86, armeabi-v7a]
- system-images;android-30;google_apis;x86_64               [x86_64, x86, arm64-v8a, armeabi-v7a]
- system-images;android-31;default;x86_64                   [x86_64]
- system-images;android-31;google_apis;x86_64               [x86_64, arm64-v8a]

FAILED IMAGES
- system-images;android-21;default;armeabi-v7a              'Failed to run emulator (Command /Users/runner/Library/Android/sdk/platform-tools/adb timed out after 180 seconds.)'
- system-images;android-22;default;armeabi-v7a              'Failed to run emulator (Command /Users/runner/Library/Android/sdk/platform-tools/adb timed out after 180 seconds.)'
- system-images;android-26;google_apis_playstore;x86        'Failed to run emulator (Command /Users/runner/Library/Android/sdk/platform-tools/adb timed out after 180 seconds.)'
- system-images;android-28;google_apis_playstore;x86_64     'Failed to run emulator (Command /Users/runner/Library/Android/sdk/platform-tools/adb timed out after 180 seconds.)'
- system-images;android-29;google_apis;arm64-v8a            'Failed to run emulator (Command /Users/runner/Library/Android/sdk/platform-tools/adb timed out after 180 seconds.)'
- system-images;android-29;google_apis_playstore;arm64-v8a  'Failed to run emulator (Command /Users/runner/Library/Android/sdk/platform-tools/adb timed out after 180 seconds.)'
- system-images;android-30;default;arm64-v8a                'Failed to run emulator (Command /Users/runner/Library/Android/sdk/platform-tools/adb timed out after 180 seconds.)'
- system-images;android-30;google_apis;arm64-v8a            'Failed to run emulator (Command /Users/runner/Library/Android/sdk/platform-tools/adb timed out after 180 seconds.)'
- system-images;android-30;google_apis_playstore;arm64-v8a  'Failed to run emulator (Command /Users/runner/Library/Android/sdk/platform-tools/adb timed out after 180 seconds.)'
- system-images;android-30;google_apis_playstore;x86        'Failed to run emulator (Command /Users/runner/Library/Android/sdk/platform-tools/adb timed out after 180 seconds.)'
- system-images;android-30;google_apis_playstore;x86_64     'Failed to run emulator (Command /Users/runner/Library/Android/sdk/platform-tools/adb timed out after 180 seconds.)'
- system-images;android-31;default;arm64-v8a                'Failed to run emulator (Command /Users/runner/Library/Android/sdk/platform-tools/adb timed out after 180 seconds.)'
- system-images;android-31;google_apis;arm64-v8a            'Failed to run emulator (Command /Users/runner/Library/Android/sdk/platform-tools/adb timed out after 180 seconds.)'
- system-images;android-31;google_apis_playstore;arm64-v8a  'Failed to run emulator (Command /Users/runner/Library/Android/sdk/platform-tools/adb timed out after 180 seconds.)'
- system-images;android-31;google_apis_playstore;x86_64     'Failed to run emulator (Command /Users/runner/Library/Android/sdk/platform-tools/adb timed out after 180 seconds.)'
""".trimIndent()

private val LinuxAmd64Inspections = """
INSPECTION RESULTS (70/70)

SUCCESSFUL IMAGES
- system-images;android-21;default;x86_64                   [x86_64, x86]
- system-images;android-21;google_apis;armeabi-v7a          [armeabi-v7a]
- system-images;android-21;google_apis;x86_64               [x86_64, x86]
- system-images;android-21;default;x86                      [x86]
- system-images;android-21;google_apis;x86                  [x86]
- system-images;android-22;default;x86_64                   [x86_64, x86]
- system-images;android-22;google_apis;armeabi-v7a          [armeabi-v7a]
- system-images;android-22;google_apis;x86_64               [x86_64, x86]
- system-images;android-22;default;x86                      [x86]
- system-images;android-22;google_apis;x86                  [x86]
- system-images;android-23;default;armeabi-v7a              [armeabi-v7a]
- system-images;android-23;default;x86_64                   [x86_64, x86]
- system-images;android-23;google_apis;armeabi-v7a          [armeabi-v7a]
- system-images;android-23;google_apis;x86_64               [x86_64, x86]
- system-images;android-23;default;x86                      [x86]
- system-images;android-23;google_apis;x86                  [x86]
- system-images;android-24;default;armeabi-v7a              [armeabi-v7a]
- system-images;android-24;default;x86_64                   [x86_64, x86]
- system-images;android-24;default;arm64-v8a                [arm64-v8a, armeabi-v7a]
- system-images;android-24;google_apis;arm64-v8a            [arm64-v8a, armeabi-v7a]
- system-images;android-24;default;x86                      [x86]
- system-images;android-24;google_apis;x86                  [x86]
- system-images;android-25;default;x86                      [x86]
- system-images;android-25;default;x86_64                   [x86_64, x86]
- system-images;android-25;google_apis;arm64-v8a            [arm64-v8a, armeabi-v7a]
- system-images;android-25;google_apis;armeabi-v7a          [armeabi-v7a]
- system-images;android-25;google_apis;x86                  [x86]
- system-images;android-25;google_apis;x86_64               [x86_64, x86]
- system-images;android-25;google_apis_playstore;x86        [x86]
- system-images;android-26;default;x86                      [x86]
- system-images;android-26;default;x86_64                   [x86_64, x86]
- system-images;android-26;google_apis;x86                  [x86]
- system-images;android-26;google_apis;x86_64               [x86_64, x86]
- system-images;android-26;google_apis_playstore;x86        [x86]
- system-images;android-27;default;x86                      [x86]
- system-images;android-27;default;x86_64                   [x86_64, x86]
- system-images;android-27;google_apis;x86                  [x86]
- system-images;android-27;google_apis_playstore;x86        [x86]
- system-images;android-28;default;x86                      [x86]
- system-images;android-28;default;x86_64                   [x86_64, x86]
- system-images;android-28;google_apis;x86                  [x86, armeabi-v7a]
- system-images;android-28;google_apis;x86_64               [x86_64, x86]
- system-images;android-28;google_apis_playstore;x86        [x86, armeabi-v7a]
- system-images;android-28;google_apis_playstore;x86_64     [x86_64, x86]
- system-images;android-29;default;x86                      [x86]
- system-images;android-29;default;x86_64                   [x86_64, x86]
- system-images;android-29;google_apis;x86                  [x86]
- system-images;android-29;google_apis;x86_64               [x86_64, x86]
- system-images;android-29;google_apis_playstore;x86        [x86]
- system-images;android-29;google_apis_playstore;x86_64     [x86_64, x86]
- system-images;android-30;default;x86_64                   [x86_64, x86]
- system-images;android-30;google_apis;x86                  [x86, armeabi-v7a]
- system-images;android-30;google_apis;x86_64               [x86_64, x86, arm64-v8a, armeabi-v7a]
- system-images;android-30;google_apis_playstore;x86        [x86, armeabi-v7a]
- system-images;android-30;google_apis_playstore;x86_64     [x86_64, x86, arm64-v8a, armeabi-v7a]
- system-images;android-31;default;x86_64                   [x86_64]
- system-images;android-31;google_apis;x86_64               [x86_64, arm64-v8a]
- system-images;android-31;google_apis_playstore;x86_64     [x86_64, arm64-v8a]

FAILED IMAGES
- system-images;android-21;default;armeabi-v7a              'Failed to run emulator (Command /opt/android-sdk-linux/platform-tools/adb timed out after 180 seconds.)'
- system-images;android-22;default;armeabi-v7a              'Failed to run emulator (Command /opt/android-sdk-linux/platform-tools/adb timed out after 180 seconds.)'
- system-images;android-24;google_apis;x86_64               'Failed to run emulator (Command /opt/android-sdk-linux/platform-tools/adb timed out after 180 seconds.)'
- system-images;android-24;google_apis_playstore;x86        'Failed to run emulator (Command /opt/android-sdk-linux/platform-tools/adb timed out after 180 seconds.)'
- system-images;android-29;google_apis;arm64-v8a            'Failed to run emulator (Command /opt/android-sdk-linux/platform-tools/adb timed out after 180 seconds.)'
- system-images;android-29;google_apis_playstore;arm64-v8a  'Failed to run emulator (Command /opt/android-sdk-linux/platform-tools/adb timed out after 180 seconds.)'
- system-images;android-30;default;arm64-v8a                'Failed to run emulator (Command /opt/android-sdk-linux/platform-tools/adb timed out after 180 seconds.)'
- system-images;android-30;google_apis;arm64-v8a            'Failed to run emulator (Command /opt/android-sdk-linux/platform-tools/adb timed out after 180 seconds.)'
- system-images;android-30;google_apis_playstore;arm64-v8a  'Failed to run emulator (Command /opt/android-sdk-linux/platform-tools/adb timed out after 180 seconds.)'
- system-images;android-31;default;arm64-v8a                'Failed to run emulator (Command /opt/android-sdk-linux/platform-tools/adb timed out after 180 seconds.)'
- system-images;android-31;google_apis;arm64-v8a            'Failed to run emulator (Command /opt/android-sdk-linux/platform-tools/adb timed out after 180 seconds.)'
- system-images;android-31;google_apis_playstore;arm64-v8a  'Failed to run emulator (Command /opt/android-sdk-linux/platform-tools/adb timed out after 180 seconds.)'
""".trimIndent()