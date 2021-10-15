package io.deepmedia.tools.testing.android.native_

import io.deepmedia.tools.testing.android.tools.Avd
import io.deepmedia.tools.testing.android.tools.ConnectedDevice
import io.deepmedia.tools.testing.android.tools.SdkPackage

internal class ArchitectureMatcher(val architecture: Architecture) {
    companion object {
        // Filter with API >= 21 is the NDK level used by K/N. It's also a way to avoid picking up
        // very old buggy images that are not maintained or compatible with latest emulator.
        private const val MIN_API = 21

        // Avoid picking up android tv or wear. Also "default" should be last: according to the footer
        // here https://android-developers.googleblog.com/2020/03/run-arm-apps-on-android-emulator.html
        // it is unlikely to have ARM translation turned on.
        private val TAGS = listOf("google_apis", "google_apis_playstore", "default")

        // Images I've had issues with
        private val BLOCKLIST = listOf(
            // This AVD's configuration is missing a kernel file!
            // Please ensure the file "kernel-ranchu" is in the same location as your system image.
            "system-images;android-21;default;armeabi-v7a",
            // Arm64 images are for Arm64 hosts, which we won't target for now.
            // https://issuetracker.google.com/issues/198022909#comment3
            // TODO fix this, tricky because sdkmanager returns unrunnable stuff
            "system-images;*;*;arm64-v8a",
        )

        private fun isInBlocklist(candidateApi: Int, candidateAbi: String, candidateTag: String): Boolean {
            val apis = listOf("android-$candidateApi", "*")
            val abis = listOf(candidateAbi, "*")
            val tags = listOf(candidateTag, "*")
            apis.forEach { api ->
                abis.forEach { abi ->
                    tags.forEach { tag ->
                        val image = "system-images;$api;$tag;$abi"
                        if (image in BLOCKLIST) return true
                    }
                }
            }
            return false
        }

        // Images that are known to accept all 4 architectures. Unfortunately we have to maintain
        // this list until we get better support: https://issuetracker.google.com/issues/202985152
        private val MAGICLIST = listOf(
            "system-images;android-30;google_apis;x86_64"
        )
    }

    private fun matches(image: SdkPackage.SystemImage) =
        image.api >= MIN_API
            && image.abi == architecture.abi
            && image.tag in TAGS
            && !isInBlocklist(image.api, image.abi, image.tag)

    private fun matches(avd: Avd) =
        avd.api >= MIN_API
            && avd.abi == architecture.abi
            && avd.tag in TAGS
            && !isInBlocklist(avd.api, avd.abi, avd.tag)

    // if tag == null, this is a device (not emulator). No blocklist for them.
    private fun matches(device: ConnectedDevice, tag: String?) =
        device.info!!.api >= MIN_API
            && architecture.abi in device.info.abiList
            && (tag == null || tag in TAGS)
            && (tag == null || !isInBlocklist(device.info.api, device.info.abi, tag))

    // Select or null

    fun selectImageOrNull(images: List<SdkPackage.SystemImage>, installedPlatforms: List<Int>) =
        selectImages(images, installedPlatforms).firstOrNull()

    fun selectAvdOrNull(avds: List<Avd>) = selectAvds(avds).firstOrNull()

    fun selectConnectedDeviceOrNull(devices: List<ConnectedDevice>, tag: (ConnectedDevice) -> String?) =
        selectConnectedDevices(devices, tag).firstOrNull()

    // Select list

    fun selectImages(images: List<SdkPackage.SystemImage>, installedPlatforms: List<Int>) = images
        .filter(::matches)
        .sortedWith(
            run {
                // If we had SystemImage.abiList, we'd sort by abiList.size here.
                val byMagic = compareByDescending<SdkPackage.SystemImage> {
                    it.id in MAGICLIST
                }
                // Being installed gives a bump but not so much, to avoid picking up
                // very old images that might have issues with modern emulator.
                val byApi = compareByDescending<SdkPackage.SystemImage> {
                    it.api + if (it.api in installedPlatforms) 4 else 0
                }
                val byValidTags = compareBy<SdkPackage.SystemImage> { TAGS.indexOf(it.tag) }
                byMagic then byApi then byValidTags
            }
        )

    fun selectAvds(avds: List<Avd>) = avds
        .filter(::matches)
        .sortedWith(
            run {
                val byApi = compareByDescending<Avd> { it.api }
                val byValidTags = compareBy<Avd> { TAGS.indexOf(it.tag) }
                byApi then byValidTags
            }
        )

    fun selectConnectedDevices(devices: List<ConnectedDevice>, tag: (ConnectedDevice) -> String?) = devices
        .filter { matches(it, tag(it)) }
        .sortedWith(
            run {
                val byApi = compareByDescending<ConnectedDevice> { it.info!!.api }
                val byValidTags = compareBy<ConnectedDevice> { TAGS.indexOf(tag(it)) }
                byApi then byValidTags
            }
        )
}