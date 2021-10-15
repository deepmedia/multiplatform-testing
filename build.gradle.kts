buildscript {
    repositories {
        google()
        mavenLocal()
        mavenCentral()
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }

    configurations.configureEach {
        resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.SECONDS)
    }

    dependencies {
        classpath("io.deepmedia.tools:publisher:0.6.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.31")
        classpath("com.android.tools.build:gradle:7.0.3")
        classpath("io.deepmedia.tools.testing:plugin:latest-SNAPSHOT") {
            isChanging = true
        }
    }
}

repositories {
    mavenCentral()
}

subprojects {
    repositories {
        google()
        mavenLocal()
        mavenCentral()
    }

    configurations.configureEach {
        resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.SECONDS)
    }

    // Configure publishable modules.
    pluginManager.withPlugin("maven-publish") {
        group = "io.deepmedia.tools.testing"
        version = "0.1.1"
    }

    /* tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).configureEach {
        kotlinOptions {
            jvmTarget = "11"
        }
    } */
}
