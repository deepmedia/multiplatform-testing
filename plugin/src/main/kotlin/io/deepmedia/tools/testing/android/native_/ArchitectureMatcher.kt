package io.deepmedia.tools.testing.android.native_

import io.deepmedia.tools.testing.android.tools.Avd
import io.deepmedia.tools.testing.android.tools.ConnectedDevice
import io.deepmedia.tools.testing.android.tools.ImageInspections
import io.deepmedia.tools.testing.android.tools.SdkPackage

internal class ArchitectureMatcher(val architecture: Architecture) {
    companion object {
        // Filter with API >= 21 is the NDK level used by K/N. It's also a way to avoid picking up
        // very old buggy images that are not maintained or compatible with latest emulator.
        private const val MIN_API = 21

        // Avoid picking up android tv or wear. Also "default" should be last: according to the footer
        // here https://android-developers.googleblog.com/2020/03/run-arm-apps-on-android-emulator.html
        // it is unlikely to have ARM translation turned on.
        private val TAGS = listOf(
            "google_atd",
            "aosp_atd",
            "google_apis",
            "google_apis_playstore",
            "default"
        )
    }

    private fun matches(image: SdkPackage.SystemImage) = image.api >= MIN_API
            && image.tag in TAGS
            && architecture.abi in ImageInspections.abiLists[image.id] ?: listOf(image.abi)
            && image.id !in ImageInspections.blocklist

    private fun matches(avd: Avd) = avd.api >= MIN_API
            && avd.tag in TAGS
            && architecture.abi in ImageInspections.getAbiList(avd.api, avd.abi, avd.tag) ?: listOf(avd.abi)
            && !ImageInspections.isInBlocklist(avd.api, avd.abi, avd.tag)

    // if tag == null, this is a device (not emulator). No blocklist for them.
    private fun matches(device: ConnectedDevice, tag: String?) = device.info!!.api >= MIN_API
            && architecture.abi in device.info.abiList
            && (tag == null || tag in TAGS)
            && (tag == null || !ImageInspections.isInBlocklist(device.info.api, device.info.abi, tag))

    // Select or null

    fun selectImageOrNull(images: List<SdkPackage.SystemImage>, installedPlatforms: List<Int>) =
        selectImages(images, installedPlatforms).firstOrNull()

    fun selectAvdOrNull(avds: List<Avd>) = selectAvds(avds).firstOrNull()

    fun selectConnectedDeviceOrNull(devices: List<ConnectedDevice>, tag: (ConnectedDevice) -> String?) =
        selectConnectedDevices(devices, tag).firstOrNull()

    // Select list

    fun selectImages(images: List<SdkPackage.SystemImage>, installedPlatforms: List<Int>) = images
        .filter(::matches)
        .sortedByDescending {
            val apiScore = it.api - MIN_API
            val tagScore = TAGS.lastIndex - TAGS.indexOf(it.tag)
            val abiScore = (ImageInspections.abiLists[it.id]?.size ?: 1) - 1
            // Being installed gives a bump but not so much, to avoid picking up
            // very old images that might have issues with modern emulator.
            val installedScore = if (it.api in installedPlatforms) 2 else 0
            apiScore + tagScore + abiScore * 2 + installedScore
        }

    fun selectAvds(avds: List<Avd>) = avds
        .filter(::matches)
        .sortedByDescending {
            val apiScore = it.api - MIN_API
            val tagScore = TAGS.lastIndex - TAGS.indexOf(it.tag)
            val abiScore = (ImageInspections.getAbiList(it.api, it.abi, it.tag)?.size ?: 1) - 1
            apiScore + tagScore + abiScore * 2
        }

    fun selectConnectedDevices(devices: List<ConnectedDevice>, tag: (ConnectedDevice) -> String?) = devices
        .filter { matches(it, tag(it)) }
        .sortedByDescending {
            val apiScore = it.info!!.api - MIN_API
            val tagScore = TAGS.lastIndex - TAGS.indexOf(tag(it)) // high if tag == null
            val abiScore = it.info.abiList.size - 1
            apiScore + tagScore + abiScore * 2
        }
}