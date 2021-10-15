package io.deepmedia.tools.testing

import io.deepmedia.tools.testing.android.native_.AndroidNativeOptions
import io.deepmedia.tools.testing.android.tools.AndroidToolsOptions
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.newInstance
import javax.inject.Inject

open class MultiplatformTestingExtension @Inject constructor(objects: ObjectFactory) {

    val androidNative: AndroidNativeOptions = objects.newInstance(AndroidNativeOptions::class)

    fun androidNative(action: Action<AndroidNativeOptions>) {
        action.execute(androidNative)
    }

    val androidTools: AndroidToolsOptions = objects.newInstance(AndroidToolsOptions::class)

    fun androidTools(action: Action<AndroidToolsOptions>) {
        action.execute(androidTools)
    }
}