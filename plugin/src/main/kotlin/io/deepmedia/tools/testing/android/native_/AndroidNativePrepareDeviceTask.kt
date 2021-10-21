package io.deepmedia.tools.testing.android.native_

import io.deepmedia.tools.testing.android.tools.*
import io.deepmedia.tools.testing.android.tools.Adb
import io.deepmedia.tools.testing.android.tools.AvdManager
import io.deepmedia.tools.testing.android.tools.Emulator
import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.property
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.io.File
import javax.inject.Inject
import kotlin.random.Random

// Can't be: @CacheableTask
open class AndroidNativePrepareDeviceTask @Inject constructor(objects: ObjectFactory) : DefaultTask() {

    @get:Input
    val architecture: Property<Architecture> = objects.property()

    @get:Input
    val sdkHome: Property<String> = objects.property()

    @get:Internal
    val device: Property<ConnectedDevice> = objects.property()

    @Option(
        option = "image",
        description = "Require a specific image."
    )
    @get:Input
    @get:Optional
    val image: Property<String> = objects.property()

    private val adb by lazy { Adb(project, sdkHome.get()) }
    private val avd by lazy { AvdManager(project, sdkHome.get()) }
    private val emulator by lazy { Emulator(project, sdkHome.get()) }
    private val sdk by lazy { SdkManager(project, sdkHome.get()) }

    init {
        // Not sure if this makes sense
        /* outputs.upToDateWhen {
            val device = device.orNull
            device != null && adb.devices().any { it.id == device.id }
        } */
    }

    @TaskAction
    fun prepareDevice() {
        val arch = architecture.get()
        val matcher = ArchitectureMatcher(arch)
        device.set(
            image.orNull?.asSystemImage()?.createAvd(true)?.connectDevice() ?:
            matcher.findConnectedDevice() ?:
            matcher.findAvd()?.connectDevice() ?:
            matcher.findSystemImage()?.createAvd(false)?.connectDevice() ?:
            error("Could not find or create a device for $arch. This architecture can't be tested.")
        )
    }

    private fun String.asSystemImage(): SdkPackage.SystemImage {
        val installed = sdk.listInstalled<SdkPackage>()
        val image = (installed.firstOrNull { it.id == this } ?: sdk.install(this).first()) as SdkPackage.SystemImage
        if (installed.none { it is SdkPackage.Platform && it.api == image.api }) {
            sdk.installPlatform(image.api)
        }
        return image
    }

    private fun ArchitectureMatcher.findConnectedDevice(): ConnectedDevice? {
        val devices = adb.devices()
        val avds = avd.list()
        println("Checking currently connected devices: $devices")
        // Could no find any way to read the system-image tag from a connected device.
        // Got to check with device.info.avdName (if it's an AVD) and then query avdmanager.
        return selectConnectedDeviceOrNull(devices) {
            it.info?.avdName?.let { name -> avds.first { it.name == name } }?.tag
        }
    }

    private fun ArchitectureMatcher.findAvd(): Avd? {
        val avds = avd.list()
        println("No running device could be used. Checking existing avds: ${avds.map { it.name }}")
        return selectAvdOrNull(avds)
    }

    private fun ArchitectureMatcher.findSystemImage(): SdkPackage.SystemImage? {
        println("No existing AVD could be used. Attempting to create a new one.")
        val installedPackages = sdk.listInstalled<SdkPackage>()
        val installedApis = installedPackages.filterIsInstance<SdkPackage.Platform>().map { it.api }

        // Try to find an image, preferring those that are already installed.
        val image = selectImageOrNull(
            images = installedPackages.filterIsInstance<SdkPackage.SystemImage>(),
            installedPlatforms = installedApis
        ) ?: selectImageOrNull(
            images = sdk.list(),
            installedPlatforms = installedApis
        )?.also {
            println("No installed images can be used to run $architecture tests. Installing from sdkmanager.")
            sdk.install(it)
        } ?: run {
            println("No image available for $architecture. This architecture can't be tested.")
            return null
        }

        // Install platform if needed.
        if (image.api !in installedApis) {
            println("Installing android-${image.api} platform runtime to match the image ${image.id}.")
            sdk.installPlatform(image.api)
        }
        return image
    }

    private fun SdkPackage.SystemImage.createAvd(check: Boolean): Avd {
        if (check) {
            val avd = avd.list().firstOrNull { it.api == api && it.tag == tag && it.abi == abi }
            if (avd != null) return avd
        }
        println("Creating AVD for $id.")
        return avd.create(this)
    }

    /**
     * After starting the emulator, we need a way to identify the device serial number in adb.
     * This is done by convention: ports go from 5554 up (increasing by 2). Another option is
     * to choose it randomly in the valid interval (5554-5584) but for CI it's better to use the
     * first for predictability - not all ports might be open. See:
     * - https://developer.android.com/studio/run/emulator-commandline#common
     * - emulator -help-port
     */
    private fun Avd.connectDevice(): ConnectedDevice {
        val usedPorts = adb.devices(onlineOnly = false).flatMap {
            it.id.takeIf { it.startsWith("emulator-") }
                ?.removePrefix("emulator-")
                ?.toInt()
                ?.let { if (it % 2 == 0) listOf(it) else listOf(it - 1, it + 1) }
                ?: emptyList()
        }
        // after identifying the port, let's also pass it to emulator.start() just for safety.
        val port = (5554 .. 5584 step 2).first { it !in usedPorts }
        val id = "emulator-$port"
        val output: File = project.file("build/multiplatformTesting/$id.log")
        val process = emulator.start(this, port, output)
        return try {
            adb.await(deviceId = id, timeout = 120L)
        } catch (e: Throwable) {
            println("Something went wrong while starting the emulator.")
            process.destroy()
            adb.printDevices()
            output.readLines().forEach { println("  $it") }
            throw e
        }
    }

    companion object {

        @OptIn(ExperimentalStdlibApi::class)
        fun taskName(target: KotlinNativeTarget) =
            "prepare${target.name.replaceFirstChar { it.uppercase() }}TestDevice"
    }
}