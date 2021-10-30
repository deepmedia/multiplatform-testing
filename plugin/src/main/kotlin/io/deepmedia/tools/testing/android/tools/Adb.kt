package io.deepmedia.tools.testing.android.tools

import org.gradle.api.Project
import java.io.File
import java.util.concurrent.TimeoutException

// https://developer.android.com/studio/command-line/adb
internal class Adb(project: Project, sdkHome: String) {

    private val terminal = Terminal(project)
    private val adb = run {
        val file = File(File(sdkHome, "platform-tools"), "adb")
        require(file.exists()) { "Could not find 'adb' in ${file.absolutePath}." }
        file.absolutePath
    }

    private val ConnectedDevice?.idArgs get() = this?.id.idArgs
    private val String?.idArgs get() = this?.let { arrayOf("-s", this) } ?: emptyArray()

    fun printDevices() {
        terminal.run(adb, "devices", "-l", timeout = 10)
    }

    fun devices(onlineOnly: Boolean = true): List<ConnectedDevice> {
        val res = terminal.run(adb, "devices", "-l", silent = true, timeout = 10)
        return res.lineSequence().drop(1)
            .filter { it.isNotBlank() }
            .map { ConnectedDevice.parse(it) }
            .filter { !onlineOnly || it.status == "device" }
            .map { if (it.status == "device") it.copy(info = deviceInfo(it)) else it }
            .toList()
    }

    private fun deviceInfo(device: ConnectedDevice): ConnectedDeviceInfo {
        val content = terminal.run(adb, *device.idArgs, "shell", "getprop", silent = true, timeout = 10)
        return ConnectedDeviceInfo.parse(content)
    }

    fun await(device: ConnectedDevice, timeout: Long): ConnectedDevice {
        terminal.run(adb, *device.idArgs, "wait-for-device", timeout = timeout)
        return device.copy(status = "device")
    }

    // Like the other signature, but can be used when you don't have a ConnectedDevice instance,
    // so even before the device shows up as offline in 'adb devices'. adb will just wait.
    fun await(deviceId: String, timeout: Long): ConnectedDevice {
        terminal.run(adb, *deviceId.idArgs, "wait-for-device", timeout = timeout)
        return devices().first { it.id == deviceId }
    }

    fun awaitBoot(device: ConnectedDevice, timeout: Long): ConnectedDevice {
        val deadline = System.currentTimeMillis() + timeout * 1000L
        var dev = device
        while (dev.info!!["sys.boot_completed"]?.trim() != "1") {
            Thread.sleep(2000)
            dev = dev.copy(info = deviceInfo(dev))
            if (System.currentTimeMillis() > deadline) {
                throw TimeoutException("sys.boot_completed check timed out.")
            }
        }
        return dev
    }

    fun push(source: String, dest: String, device: ConnectedDevice? = null) {
        terminal.run(adb, *device.idArgs, "push", source, dest, timeout = 10)
    }

    fun run(command: String, device: ConnectedDevice? = null, timeout: Long): String {
        return terminal.run(adb, *device.idArgs, "shell", command, timeout = timeout)
    }

    fun emu(command: String, device: ConnectedDevice) {
        requireNotNull(device.info?.avdName) { "Device $device is not an emulator." }
        terminal.run(adb, *device.idArgs, "emu", command, timeout = 10)
    }

    fun putSetting(device: ConnectedDevice, key: String, value: String) {
        requireNotNull(device.info?.avdName) { "Device $device is not an emulator." }
        terminal.run(adb, *device.idArgs, "shell", "settings", "put", "global", key, value, timeout = 5)
    }

    fun disableAnimations(device: ConnectedDevice) {
        putSetting(device, "window_animation_scale", "0.0")
        putSetting(device, "transition_animation_scale", "0.0")
        putSetting(device, "animator_duration_scale", "0.0")
    }

    fun unlock(device: ConnectedDevice) {
        terminal.run(adb, *device.idArgs, "shell", "input", "keyevent", "82", timeout = 5)
    }
}