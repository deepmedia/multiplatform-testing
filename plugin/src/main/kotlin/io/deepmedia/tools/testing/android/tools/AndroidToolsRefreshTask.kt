package io.deepmedia.tools.testing.android.tools

import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

open class AndroidToolsRefreshTask @Inject constructor(objects: ObjectFactory) : DefaultTask() {

    companion object {
        fun taskName() = "refreshAndroidTools"
    }

    @get:Input
    val sdkHome: Property<String> = objects.property()

    init {
        // Let's say that after running once, we're fine.
        outputs.upToDateWhen { true }
    }

    @TaskAction
    fun refresh() {
        // This assumes that cmdline-tools are already installed. We don't support the old
        // sdkmanager because updating to cmdline-tools might fail depending on the java version.
        var sdk = SdkManager(project, sdkHome.get())
        val available = sdk.list<SdkPackage>()
        val installed = sdk.listInstalled<SdkPackage>()

        // Update cmdline-tools;latest. This includes the sdkmanager itself and avdmanager.
        val cmdLine0 = available.filterIsInstance<SdkPackage.CmdLineTools>().first { it.latest }
        val cmdLine1 = installed.filterIsInstance<SdkPackage.CmdLineTools>().lastOrNull() // last should have higher version
        println("cmdline-tools: installed=${cmdLine1?.version} latest=${cmdLine0.version}")
        if (cmdLine1?.version != cmdLine0.version) {
            sdk.install(cmdLine0)
            val latest2 = SdkManager.cmdlineToolsPath(sdkHome.get(), "latest-2")
            if (latest2.exists()) {
                println("cmdline-tools: installed to latest-2! Fixing the installation.")
                val latest = SdkManager.cmdlineToolsPath(sdkHome.get(), "latest")
                with(project) {
                    delete { delete(latest) }
                    copy {
                        from(latest2)
                        into(latest)
                    }
                    delete { delete(latest2) }
                }
            }
            sdk = SdkManager(project, sdkHome.get())
        }
        // sdk.acceptLicenses()

        // Update platform-tools which includes adb.
        val platform0 = available.filterIsInstance<SdkPackage.PlatformTools>().single()
        val platform1 = installed.filterIsInstance<SdkPackage.PlatformTools>().singleOrNull()
        println("platform-tools: installed=${platform1?.version} latest=${platform0.version}")
        if (platform1?.version != platform0.version) {
            sdk.install(platform0)
        }

        // Update the emulator.
        val emu0 = available.filterIsInstance<SdkPackage.Emulator>().single()
        val emu1 = installed.filterIsInstance<SdkPackage.Emulator>().singleOrNull()
        println("emulator: installed=${emu1?.version} latest=${emu0.version}")
        if (emu1?.version != emu0.version) {
            sdk.install(emu0)
        }
    }
}