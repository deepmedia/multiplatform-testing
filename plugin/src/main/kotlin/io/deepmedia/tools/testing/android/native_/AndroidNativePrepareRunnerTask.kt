package io.deepmedia.tools.testing.android.native_

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.property
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import javax.inject.Inject

@CacheableTask
open class AndroidNativePrepareRunnerTask @Inject constructor(objects: ObjectFactory) : DefaultTask() {

    @get:Input
    val architecture: Property<Architecture> = objects.property()

    @get:Input
    val targetName: Property<String> = objects.property()

    @get:OutputFile
    val runner: RegularFileProperty = objects.fileProperty().convention(targetName.flatMap { target ->
        project.layout.buildDirectory.file("multiplatformTesting/$target/runner")
    })

    @TaskAction
    fun prepare() {
        val abi = architecture.get().abi
        val stream = this::class.java.classLoader.getResourceAsStream("$abi/runner")!!
        stream.copyTo(runner.get().asFile.outputStream())
    }

    companion object {

        @OptIn(ExperimentalStdlibApi::class)
        fun taskName(target: KotlinNativeTarget) =
            "prepare${target.name.replaceFirstChar { it.uppercase() }}TestRunner"
    }
}