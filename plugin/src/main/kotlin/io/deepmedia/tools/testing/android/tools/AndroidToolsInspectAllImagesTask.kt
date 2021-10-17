package io.deepmedia.tools.testing.android.tools

import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.property
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

open class AndroidToolsInspectAllImagesTask @Inject constructor(objects: ObjectFactory) : DefaultTask() {

    companion object {
        fun taskName() = "inspectAllAndroidImages"
        private val VALID_ABIS = listOf("x86_64", "x86", "arm64-v8a", "armeabi-v7a")
        private val VALID_TAGS = listOf("default", "google_apis", "google_apis_playstore")
    }

    @get:Input
    val sdkHome: Property<String> = objects.property()

    @Option(option = "min_api", description = "Min API level.")
    @get:Input
    val minApi: Property<String> = objects.property<String>().convention(21.toString()) // int options not supported

    @Option(option = "max_api", description = "Max API level.")
    @get:Input
    val maxApi: Property<String> = objects.property<String>().convention(Int.MAX_VALUE.toString()) // int options not supported

    @Option(option = "abi_list", description = "Comma-separated list of abis to check.")
    @get:Input
    val abiList: Property<String> = objects.property<String>().convention(VALID_ABIS.joinToString(","))

    @Option(option = "tag_list", description = "Comma-separated list of tags to check.")
    @get:Input
    val tagList: Property<String> = objects.property<String>().convention(VALID_TAGS.joinToString(","))

    private val sdk by lazy { SdkManager(project, sdkHome.get()) }
    private val avd by lazy { AvdManager(project, sdkHome.get()) }
    private val emu by lazy { Emulator(project, sdkHome.get()) }
    private val adb by lazy { Adb(project, sdkHome.get()) }

    private data class Info(
        val image: String,
        val abiList: List<String>,
        val exception: String?
    )

    private fun List<Info>.print(expected: Int) {
        val partial = size != expected
        println("\n----------------------------")
        if (partial) {
            println("PARTIAL INSPECTION RESULTS ($size/$expected)")
        } else {
            println("FINAL INSPECTION RESULTS ($size images)")
        }
        println("----------------------------\n")
        val success = filter { it.exception == null }.sortedByDescending { it.abiList.size }
        val failure = filter { it.exception != null }
        println("SUCCESSFUL IMAGES (${success.size}/${size})")
        success.forEach {
            println("- ${it.image} ${it.abiList}")
        }
        println()
        println("FAILED IMAGES (${failure.size}/${size})")
        failure.forEach {
            println("- ${it.image} '${it.exception}'")
        }
        println("\n----------------------------\n")
    }

    @TaskAction
    fun inspect() {
        val abis = abiList.get().split(',').filter { it in VALID_ABIS }
        val tags = tagList.get().split(',').filter { it in VALID_TAGS }
        val images = sdk.list<SdkPackage.SystemImage>().filter {
            it.api in minApi.get().toInt() .. maxApi.get().toInt() && it.abi in abis && it.tag in tags
        }
        println("INSPECTING ${images.size} IMAGES")
        val results = mutableListOf<Info>()
        images.forEachIndexed { i, it ->
            println("INSPECTING IMAGE ${it.id} (${(i + 1)}/${images.size})")
            results.add(inspect(it))
            deinits.reversed().forEach { it.invoke() }
            deinits.clear()
            results.print(images.size)
        }
    }

    private val deinits = mutableListOf<() -> Unit>()

    private inline fun <T> SdkPackage.SystemImage.step(
        op: String,
        init: () -> T,
        crossinline deinit: (T) -> Unit,
        err: (Info) -> Nothing
    ): T {
        println("$id: STEP '$op' STARTED.")
        val res = try {
            init()
        } catch (e: Throwable) {
            println("$id: STEP '$op' FAILED.")
            deinits.reversed().forEach { it.invoke() }
            deinits.clear()
            err(Info(id, emptyList(), "Failed to $op (${e.message ?: e::class.simpleName ?: "unknown"})"))
        }
        deinits.add { deinit(res) }
        return res
    }

    private fun inspect(image: SdkPackage.SystemImage): Info {
        if (sdk.listInstalled<SdkPackage.Platform>().none { it.api == image.api }) {
            image.step("install platform", { sdk.installPlatform(image.api) }, { }, { return it })
        }

        if (sdk.listInstalled<SdkPackage.SystemImage>().none { it.id == image.id }) {
            image.step("install image", { sdk.install(image) }, { sdk.uninstall(image) }, { return it })
        }

        val device = image.step("create avd", { avd.create(image) }, { avd.delete(it) }, { return it })

        println("${image.id}: Running emulator.")
        val output: File = project.file("build/multiplatformTesting/${device.name}.log")
        var process: Process? = null
        val connected = image.step("run emulator",
            init = {
                process = emu.start(device, 5554, output)
                println("${image.id}: Waiting for device to get online.")
                adb.await("emulator-5554", 180L)
            },
            deinit = {
                adb.emu("kill", it)
                output.delete()
            },
            err = {
                runCatching { adb.printDevices() }
                runCatching { process?.destroyForcibly()?.waitFor(10, TimeUnit.SECONDS) }
                runCatching { process?.destroy() }
                output.readLines().forEach { println("  $it") }
                output.delete()
                return it
            },
        )
        return Info(
            image = image.id,
            abiList = connected.info!!.abiList,
            exception = null
        )
    }
}