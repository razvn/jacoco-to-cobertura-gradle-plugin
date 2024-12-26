package net.razvan

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.ConsoleRenderer
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.testing.jacoco.plugins.JacocoPlugin
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.simpleframework.xml.Serializer
import org.simpleframework.xml.core.Persister
import org.simpleframework.xml.stream.Format
import java.io.File

class JacocoToCoberturaPlugin : Plugin<Project> {
    override fun apply(project: Project) = project.run {
        val coberturaTask = tasks.register<JacocoToCoberturaTask>(TASK_NAME) {
            outputFile.fileProvider(inputFile.map { inputFile ->
                inputFile.asFile.parentFile.resolve("cobertura-${inputFile.asFile.nameWithoutExtension}.xml")
            })

            splitByPackage.convention(false)
        }

        plugins.withType<JacocoPlugin>().configureEach {
            tasks.withType<JacocoReport>().singleOrNull()?.let { jacocoTask ->
                coberturaTask.configure {
                    /*
                    We'd like to use the following to set up the task dependency at the same time as
                    we set up the value, but due to https://github.com/gradle/gradle/issues/6619 we
                    get the following error instead:

                    > Property 'outputLocation' is declared as an output property of Report xml (type
                    TaskGeneratedSingleFileReport) but does not have a task associated with it.
                    */
                    // inputFile.convention(jacocoTask.reports.xml.outputLocation)

                    dependsOn(jacocoTask)
                    inputFile.convention(jacocoTask.reports.xml.outputLocation.locationOnly)

                    sourceDirectories.from(jacocoTask.sourceDirectories)
                }
            }
        }
    }

    companion object {
        const val TASK_NAME = "jacocoToCobertura"
    }
}

abstract class JacocoToCoberturaTask : DefaultTask() {
    @get:InputFile
    abstract val inputFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    abstract val sourceDirectories: ConfigurableFileCollection

    @get:Input
    abstract val splitByPackage: Property<Boolean>

    init {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
    }

    @TaskAction
    fun convert() {
        val consoleRenderer = ConsoleRenderer()

        logger.debug("Converting JaCoCo report to Cobertura")

        val input = inputFile.asFile.get()
            .also {
                if (!it.exists()) throw JacocoToCoberturaException(
                    "File `${it.absolutePath}` does not exist"
                )
            }
        val output = outputFile.asFile.get()
            .also {
                if (!it.parentFile.exists()) {
                    try {
                        if (!it.parentFile.mkdirs()) throw JacocoToCoberturaException("`mkdirs()` returned false")
                    } catch (e: Exception) {
                        throw JacocoToCoberturaException("Output file directory `${it.parentFile.canonicalPath} does not exists and couldn't be created, error: `${e.message}`")
                    }
                }
            }

        val sourceDirectories = sourceDirectories.files.map { it.absolutePath }
        val splitByPackage = splitByPackage.getOrElse(false)

        logger.debug(
            "Calculated configuration: input: {}, output: {}, splitByPackage: {}, sourceDirs: {}",
            input,
            output,
            splitByPackage,
            sourceDirectories.joinToString("\n", "\n")
        )

        val jacocoData = loadJacocoData(input)

        if (splitByPackage) {
            jacocoData.packages.forEach { packageElement ->
                val packageName = packageElement.name?.replace('/', '.')
                val packageData = jacocoData.copy(packages = listOf(packageElement))
                val packageOut = File(output.absolutePath.replace(".xml", "-${packageName}.xml"))
                writeCoberturaData(packageOut, transformData(packageData, sourceDirectories))
                logger.lifecycle("Cobertura report for package $packageName generated at ${consoleRenderer.asClickableFileUrl(packageOut)}")
            }
        } else {
            writeCoberturaData(output, transformData(jacocoData, sourceDirectories))
            logger.lifecycle("Cobertura report generated at ${consoleRenderer.asClickableFileUrl(output)}")
        }
    }

    private fun loadJacocoData(fileIn: File): Jacoco.Report = try {
        val serializer: Serializer = Persister()
        serializer.read(Jacoco.Report::class.java, fileIn)
    } catch (e: Exception) {
        throw JacocoToCoberturaException("Loading Jacoco report error: `${e.message}`")
    }

    private fun transformData(jacocoData: Jacoco.Report, sources: Collection<String>) = try {
        Cobertura.Coverage(jacocoData, sources)
    } catch (e: Exception) {
        throw JacocoToCoberturaException("Transforming Jacoco Data to Cobertura error: `${e.message}`")
    }

    private fun writeCoberturaData(outputFile: File, data: Cobertura.Coverage) = with(outputFile) {
        try {
            Persister(Format("<?xml version=\"1.0\" encoding= \"UTF-8\" ?>")).write(data, this)
        } catch (e: Exception) {
            throw JacocoToCoberturaException("Writing Cobertura Data to file `${this.canonicalPath}` error: `${e.message}`")
        }
    }
}

private class JacocoToCoberturaException(msg: String) : Exception(msg)
