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
    val abiList: Property<String> = objects.property<String>().convention("x86_64,x86,armeabi-v7a,arm64-v8a")

    @Option(option = "tag_list", description = "Comma-separated list of tags to check.")
    @get:Input
    val tagList: Property<String> = objects.property<String>().convention("google_apis,google_apis_playstore,default")

    private data class Result(
        val image: String,
        val abiList: List<String>,
        val exception: String?
    )

    private val sdk by lazy { SdkManager(project, sdkHome.get()) }
    private val avd by lazy { AvdManager(project, sdkHome.get()) }
    private val emu by lazy { Emulator(project, sdkHome.get()) }
    private val adb by lazy { Adb(project, sdkHome.get()) }

    @TaskAction
    fun inspect() {
        val images = sdk.list<SdkPackage.SystemImage>().filter {
            it.api in minApi.get().toInt() .. maxApi.get().toInt()
                    && it.abi in abiList.get().split(',')
                    && it.tag in tagList.get().split(',')
        }
        println("INSPECTING ${images.size} IMAGES")
        val results = images.mapIndexed { i, it ->
            println("INSPECTING IMAGE ${(i + 1)}/${images.size}")
            inspect(it)
        }
        println("------------------")
        println("INSPECTION RESULTS")
        println("------------------")
        println()
        val success = results.filter { it.exception == null }.sortedByDescending { it.abiList.size }
        val failure = results.filter { it.exception != null }
        println("SUCCESSFUL IMAGES (${success.size}/${results.size})")
        success.forEach {
            println("- ${it.image} ${it.abiList}")
        }
        println()
        println("FAILED IMAGES (${failure.size}/${results.size})")
        failure.forEach {
            println("- ${it.image} '${it.exception}'")
        }
    }

    private fun inspect(image: SdkPackage.SystemImage): Result {
        if (sdk.listInstalled<SdkPackage.SystemImage>().none { it.id == image.id }) {
            println("${image.id}: Installing image.")
            sdk.install(image)
        }
        if (sdk.listInstalled<SdkPackage.Platform>().none { it.api == image.api }) {
            println("${image.id}: Installing platform ${image.api}.")
            sdk.installPlatform(image.api)
        }
        println("${image.id}: Creating AVD.")
        val device = avd.create(image)

        println("${image.id}: Running emulator.")
        val output: File = project.file("build/multiplatformTesting/${device.name}.log")
        var process: Process? = null
        return try {
            process = emu.start(device, 5554, output)
            println("${image.id}: Waiting for device to get online.")
            val connected = adb.await("emulator-5554", 100L)
            adb.emu("kill", connected)
            Result(
                image = image.id,
                abiList = connected.info!!.abiList,
                exception = null
            )
        } catch (e: Throwable) {
            println("${image.id}: Something went wrong.")
            runCatching { adb.printDevices() }
            runCatching { process?.destroyForcibly()?.waitFor(10, TimeUnit.SECONDS) }
            runCatching { process?.destroy() }
            output.readLines().forEach { println("  $it") }
            Result(
                image = image.id,
                abiList = emptyList(),
                exception = "${e::class.simpleName} ${e.message}"
            )
        } finally {
            avd.delete(device)
            sdk.uninstall(image)
            output.delete()
        }
    }
}