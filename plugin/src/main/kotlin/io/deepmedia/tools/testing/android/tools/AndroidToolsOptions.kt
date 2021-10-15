package io.deepmedia.tools.testing.android.tools

import org.gradle.api.file.FileSystemOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

// https://docs.gradle.org/current/userguide/custom_gradle_types.html#service_injection
open class AndroidToolsOptions @Inject constructor(
    objects: ObjectFactory,
    providers: ProviderFactory
) {
    val sdkHome: Property<String> = objects
        .property<String>()
        .convention(providers.environmentVariable("ANDROID_HOME"))
}