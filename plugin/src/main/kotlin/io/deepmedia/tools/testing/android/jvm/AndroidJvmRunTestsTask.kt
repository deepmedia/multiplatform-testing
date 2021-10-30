package io.deepmedia.tools.testing.android.jvm

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget

internal object AndroidJvmRunTestsTask {

    @OptIn(ExperimentalStdlibApi::class)
    fun taskName(target: KotlinAndroidTarget) =
        "run${target.name.replaceFirstChar { it.uppercase() }}Tests"
}