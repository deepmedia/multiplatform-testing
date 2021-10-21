import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("kotlin-multiplatform")
    id("io.deepmedia.tools.multiplatform-testing")
}

fun KotlinMultiplatformExtension.androidNative(
    config: KotlinNativeTarget.() -> Unit = {}
) {
    val androidNativeMain by sourceSets.creating {
        dependsOn(sourceSets.commonMain.get())
    }
    val androidNativeTest by sourceSets.creating {
        dependsOn(sourceSets.commonTest.get())
    }
    val wrapper: KotlinNativeTarget.() -> Unit = {
        compilations["main"].defaultSourceSet.dependsOn(androidNativeMain)
        compilations["test"].defaultSourceSet.dependsOn(androidNativeTest)
        config()
    }
    androidNativeX64(configure = wrapper)
    androidNativeX86(configure = wrapper)
    androidNativeArm32(configure = wrapper)
    androidNativeArm64(configure = wrapper)
}

kotlin {
    androidNative()
    jvm()
    js { browser() }
    linuxX64 { }

    sourceSets {
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }

    targets.withType(KotlinNativeTarget::class).configureEach {
        binaries.configureEach {
            freeCompilerArgs += listOf("-Xverbose-phases=ObjectFiles,Linker")
        }
    }
}