package io.deepmedia.tools.testing.android.tools

import java.io.File

data class Avd(
    val name: String,
    val hardware: String?,
    val path: String,
    val target: String,
    val tag: String,
    val abi: String,
    val skin: String?,
    val sdCard: String,
) {
    // Read from the configuration file
    val api by lazy {
        val config = path.replace(".avd", ".ini")
        val target = File(config).readLines().associate {
            val (k, v) = it.split('=')
            k to v
        }["target"]
        target!!.removePrefix("android-").toInt()
    }

    companion object {
        /**
         * avdmanager list avd returns a list of:
         *     Name: Pixel_4_API_30
         *   Device: pixel_4 (Google)
         *     Path: /Users/natario/.android/avd/Pixel_4_API_30.avd
         *   Target: Google Play (Google Inc.)
         *           Based on: Android 11.0 (R) Tag/ABI: google_apis_playstore/x86
         *     Skin: pixel_4
         *   Sdcard: 512M
         * separated by ---------.
         */
        internal fun parse(content: String): Avd {
            val map = content.lines().filter { it.isNotBlank() }.associate {
                val key = it.split(':')[0]
                val value = it.removePrefix("$key:")
                key.trim() to value.trim()
            }
            val info = map["Based on"]!!
            val (tag, abi) = info.split(':').last().split('/')
            return Avd(
                name = map["Name"]!!,
                hardware = map["Device"],
                path = map["Path"]!!,
                target = map["Target"]!!,
                tag = tag.trim(),
                abi = abi.trim(),
                skin = map["Skin"],
                sdCard = map["Sdcard"]!!
            )
        }
    }
}