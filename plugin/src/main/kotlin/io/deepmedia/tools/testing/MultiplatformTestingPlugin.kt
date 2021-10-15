package io.deepmedia.tools.testing

import io.deepmedia.tools.testing.android.native_.registerAndroidNativeTasks
import io.deepmedia.tools.testing.android.tools.registerAndroidToolsTasks
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family

@Suppress("UnstableApiUsage")
class MultiplatformTestingPlugin : Plugin<Project> {

    companion object {
        internal const val TASK_GROUP = "Multiplatform Testing"
    }

    override fun apply(target: Project) {
        val kotlin = target.extensions.getByName("kotlin") as KotlinProjectExtension
        if (kotlin is KotlinMultiplatformExtension) {
            val extension = target.extensions.create<MultiplatformTestingExtension>("multiplatformTesting")

            // Android Tools
            target.registerAndroidToolsTasks(extension.androidTools)

            // Android Native
            target.registerAndroidNativeTasks(kotlin, extension.androidNative, extension.androidTools)
        }
    }
}