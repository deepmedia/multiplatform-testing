import io.deepmedia.tools.publisher.common.Release
import io.deepmedia.tools.publisher.common.GithubScm
import io.deepmedia.tools.publisher.common.License
import io.deepmedia.tools.publisher.sonatype.Sonatype

plugins {
    `kotlin-dsl`
    id("io.deepmedia.tools.publisher")
}

// https://docs.gradle.org/current/userguide/cross_project_publications.html
val runners by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "1.8"
        // Prevent issue when running on old versions of Gradle that use old Kotlin
        // https://youtrack.jetbrains.com/issue/KT-30330
        // apiVersion = "1.3"
        // languageVersion = "1.3"
    }
}

dependencies {
    api(gradleApi())
    api(gradleKotlinDsl())
    api("org.jetbrains.kotlin:kotlin-gradle-plugin-api:1.5.31")
    api("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.31")
    // api("com.android.tools.build:gradle:7.0.2")
    add(runners.name, project(":runner-dlopen", "output"))
}

// Tried to do the same with kotlin, but it doesn't work - resources not bundled.
val java = convention.getPlugin(JavaPluginConvention::class)
java.sourceSets {
    this["main"].resources.srcDir(runners)
}

publisher {
    project.name = "Multiplatform Testing Plugin"
    project.description = "A Gradle plugin to ease testing in Kotlin Multiplatform projects."
    project.url = "https://github.com/deepmedia/multiplatform-testing"
    project.scm = GithubScm("deepmedia", "multiplatform-testing")
    project.addLicense(License.APACHE_2_0)
    project.addDeveloper(
        name = "natario1",
        email = "mattia@deepmedia.io",
        organization = "DeepMedia",
        url = "deepmedia.io"
    )

    directory()

    directory("snapshot") {
        release.version = "latest-SNAPSHOT"
    }

    sonatype {
        auth.user = "SONATYPE_USER"
        auth.password = "SONATYPE_PASSWORD"
        signing.key = "SIGNING_KEY"
        signing.password = "SIGNING_PASSWORD"
        release.sources = Release.SOURCES_AUTO
        release.docs = Release.DOCS_AUTO
    }

    sonatype("snapshot") {
        repository = Sonatype.OSSRH_SNAPSHOT_1
        release.version = "latest-SNAPSHOT"
        auth.user = "SONATYPE_USER"
        auth.password = "SONATYPE_PASSWORD"
        signing.key = "SIGNING_KEY"
        signing.password = "SIGNING_PASSWORD"
        release.sources = Release.SOURCES_AUTO
        release.docs = Release.DOCS_AUTO
    }

    // Could also publish a marker for applying the plugin easily, in the form:
    // $id:$id.gradle.plugin:$version . Should have no code and a single dependency
    // on this plugin.
    val marker: io.deepmedia.tools.publisher.Publication.() -> Unit = {
        project.group = "io.deepmedia.tools.multiplatform-testing"
        project.artifact = "${project.group}.gradle.plugin"
    }
}