package io.deepmedia.tools.testing.android.tools

import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

open class AndroidToolsKillEmulatorsTask @Inject constructor(objects: ObjectFactory) : DefaultTask() {

    companion object {
        fun taskName() = "killAndroidEmulators"
    }

    @get:Input
    val sdkHome: Property<String> = objects.property()

    @TaskAction
    fun killEmulators() {
        val adb = Adb(project, sdkHome.get())
        adb.devices().filter {
            it.info?.avdName != null
        }.forEach {
            adb.emu("kill", it)
        }
    }
}