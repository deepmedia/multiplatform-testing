package io.deepmedia.tools.testing.android.jvm

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

open class AndroidJvmOptions @Inject constructor(
    objects: ObjectFactory,
    providers: ProviderFactory,
) {

    val apiLevel: Property<Int> = objects
        .property<Int>()
        .convention(providers
            .environmentVariable("MPT_ANDROID_API")
            .map { it.toInt() }
        )

    val tag: Property<String> = objects
        .property<String>()
        .convention(providers
            .environmentVariable("MPT_ANDROID_TAG")
        )

    val includeUnitTests: Property<Boolean> = objects
        .property<Boolean>()
        .convention(true)

    val defaultVariant: Property<String> = objects
        .property<String>()
        .convention(providers
            .environmentVariable("MPT_ANDROID_VARIANT"))
}