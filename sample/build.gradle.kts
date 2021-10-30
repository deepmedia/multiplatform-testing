import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("kotlin-multiplatform")
    id("io.deepmedia.tools.multiplatform-testing")
    id("com.android.library")
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
android {
    compileSdk = 31
    defaultConfig {
        minSdk = 21
        targetSdk = 31
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    sourceSets {
        getByName("main") {
            setRoot("src/androidMain")
        }
        getByName("test") {
            // For some reason setRoot and 'kotlin' don't work, got to modify 'java'
            java.setSrcDirs(listOf("src/androidUnitTest/kotlin"))
        }
        getByName("androidTest") {
            // For some reason setRoot and 'kotlin' don't work, got to modify 'java'
            java.setSrcDirs(listOf("src/androidDeviceTest/kotlin"))
        }
    }
}

kotlin {
    android()
    androidNative {
        binaries {
            getTest(DEBUG).apply {
                // freeCompilerArgs += listOf("-Xbinary=androidProgramType=standalone")
                // freeCompilerArgs += listOf("-Xbinary=androidProgramType=nativeActivity")
            }
        }
    }
    jvm()
    js { browser() }
    linuxX64 { }

    sourceSets {
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        getByName("androidTest") {
            dependencies {
                implementation(kotlin("test-junit"))
                // implementation("androidx.test:core-ktx:1.4.0") robolectric
            }
        }
        getByName("androidAndroidTest") {
            dependencies {
                implementation("androidx.test:core-ktx:1.4.0")
                implementation("androidx.test:rules:1.4.0")
                implementation("androidx.test:runner:1.4.0")
                implementation("androidx.test.ext:junit-ktx:1.1.3")
            }
        }
    }

    targets.withType(KotlinNativeTarget::class).configureEach {
        binaries.configureEach {
            freeCompilerArgs += listOf("-Xverbose-phases=ObjectFiles,Linker")
        }
    }
}
