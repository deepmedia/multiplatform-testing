package io.deepmedia.tools.testing.android.native_

import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

open class AndroidNativeOptions @Inject constructor(objects: ObjectFactory) {

    // No options for now.

    /* internal val x64Enabled = objects.property<Boolean>().convention(false)
    fun enableX64() {
        x64Enabled.set(true)
    } */
}