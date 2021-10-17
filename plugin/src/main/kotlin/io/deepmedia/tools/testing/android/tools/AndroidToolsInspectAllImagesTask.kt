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
        fun taskName(): String = "inspectAllAndroidImages"
        private val VALID_ABIS = listOf("x86_64", "x86", "arm64-v8a", "armeabi-v7a")
        private val VALID_TAGS = listOf("default", "google_apis", "google_apis_playstore")
    }

    @get:Input
    val sdkHome: Property<String> = objects.property()

    @Option(option = "drop", description = "Number of images to skip. Useful for retries.")
    @get:Input
    val drop: Property<String> = objects.property<String>().convention(0.toString()) // int options not supported

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

    private fun List<Info>.print(expected: Int, dropped: Int) {
        val size = size + dropped
        val partial = size != expected
        println("\n----------------------------")
        if (partial) {
            println("PARTIAL INSPECTION RESULTS ($size/$expected)")
        } else {
            println("FINAL INSPECTION RESULTS ($size images)")
        }
        println("----------------------------\n")
        if (dropped > 0) {
            println("DROPPED IMAGES ($dropped/$size)")
            println()
        }
        val success = filter { it.exception == null }
        val failure = filter { it.exception != null }
        println("SUCCESSFUL IMAGES (${success.size}/$size)")
        success.forEach {
            println("- ${it.image} ${it.abiList}")
        }
        println()
        println("FAILED IMAGES (${failure.size}/$size)")
        failure.forEach {
            println("- ${it.image} '${it.exception}'")
        }
        println("\n----------------------------\n")
    }

    @TaskAction
    fun inspect() {
        val apis = minApi.get().toInt() .. maxApi.get().toInt()
        val abis = abiList.get().split(',').filter { it in VALID_ABIS }
        val tags = tagList.get().split(',').filter { it in VALID_TAGS }
        val offset = drop.get().toInt()
        val groups = sdk.list<SdkPackage.SystemImage>()
            .filter {
                it.api in apis && it.abi in abis && it.tag in tags
            }
            .drop(offset)
            .groupBy { it.api }
        val count = offset + groups.values.flatten().size

        println("INSPECTING $count IMAGES")
        val results = mutableListOf<Info>()
        repeat(offset) {
            println("DROPPING IMAGE (${it + 1}/$count)")
        }
        groups.forEach { (api, images) ->
            images.forEach {
                println("INSPECTING IMAGE ${it.id} (${offset + results.size + 1}/$count)")
                results.add(inspect(it))
                deinits.reversed().forEach { it.invoke() }
                deinits.clear()
                results.print(dropped = offset, expected = count)
            }
            sdk.uninstallPlatform(api)
        }
    }

    private val deinits = mutableListOf<() -> Unit>()

    private inline fun <T> SdkPackage.SystemImage.step(
        op: String,
        init: () -> T,
        crossinline deinit: (T) -> Unit,
        err: (Info) -> Nothing
    ): T {
        println("STEP '$op' STARTED ($id).")
        val res = try {
            init()
        } catch (e: Throwable) {
            println("STEP '$op' FAILED ($id).")
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