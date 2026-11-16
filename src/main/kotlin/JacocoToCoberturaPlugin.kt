package net.razvan

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.file.ProjectLayout
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.testing.jacoco.plugins.JacocoPlugin
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.io.File
import javax.inject.Inject

class JacocoToCoberturaPlugin : Plugin<Project> {
    override fun apply(project: Project) = project.run {
        val coberturaTask = tasks.register<JacocoToCoberturaTask>(TASK_NAME) {
            outputFile.fileProvider(inputFile.map { input ->
                input.asFile.parentFile.resolve("cobertura-${input.asFile.nameWithoutExtension}.xml")
            })

            splitByPackage.convention(false)
            rootPackageToRemove.convention("")
        }

        plugins.withType<JacocoPlugin>().configureEach {
            val jacocoReportTasks = tasks.withType<JacocoReport>()
            val jacocoTask = jacocoReportTasks.singleOrNull()
            if (jacocoTask != null) {
                coberturaTask.configure {
                    /*
                    We'd like to use the following to set up the task dependency at the same time as
                    we set up the value, but due to https://github.com/gradle/gradle/issues/6619 we
                    get the following error instead:

                    > Property 'outputLocation' is declared as an output property of Report xml (type
                    TaskGeneratedSingleFileReport) but does not have a task associated with it.
                    */
                    // it.inputFile.convention(jacocoTask.reports.xml.outputLocation)

                    it.dependsOn(jacocoTask)
                    it.inputFile.convention(jacocoTask.reports.xml.outputLocation.locationOnly)

                    it.sourceDirectories.from(jacocoTask.sourceDirectories)
                }
            } else if (jacocoReportTasks.any()) {
                logger.warn(
                    "Multiple jacoco XML report tasks found; skipping auto-configuration of $TASK_NAME. " +
                        "Configure its inputFile and sourceDirectories manually."
                )
            }
        }
    }

    companion object {
        const val TASK_NAME = "jacocoToCobertura"
    }
}

@CacheableTask
abstract class JacocoToCoberturaTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:OutputDirectory
    abstract val splitOutputDirectory: DirectoryProperty

    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    abstract val sourceDirectories: ConfigurableFileCollection

    @get:Input
    abstract val splitByPackage: Property<Boolean>

    @get:Optional
    @get:Input
    abstract val rootPackageToRemove: Property<String>

    @get:Inject
    protected abstract val layout: ProjectLayout

    init {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        splitOutputDirectory.convention(
            // locationOnly: Gradle evaluates output conventions before the task runs,
            // where querying the task's own mapped outputFile is not allowed
            layout.dir(outputFile.locationOnly.map { splitDirFor(it.asFile) })
        )
    }

    private fun splitDirFor(outputFile: File): File =
        outputFile.parentFile.resolve("${outputFile.nameWithoutExtension}-packages")

    @TaskAction
    fun convert() {
        logger.debug("Converting JaCoCo report to Cobertura")

        val input = inputFile.asFile.get()
        val output = outputFile.asFile.get()
            .also {
                val parentFile = it.parentFile
                    ?: throw JacocoToCoberturaException("Parent path of file directory `${it.canonicalPath}` is null.")
                if (!parentFile.exists()) {
                    try {
                        if (!parentFile.mkdirs()) throw JacocoToCoberturaException("`mkdirs()` returned false")
                    } catch (e: Exception) {
                        throw JacocoToCoberturaException("Output file directory `${parentFile.canonicalPath}` does not exists and couldn't be created, error: `${e.message ?: "unknown error"}`")
                    }
                }
            }

        val sourceDirectories = sourceDirectories.files.map { it.absolutePath }
        val splitByPackage = splitByPackage.getOrElse(false)
        val rootPackage = rootPackageToRemove.getOrElse("").takeIf { it.isNotBlank() }

        logger.debug(
            "Calculated configuration: input: {}, output: {}, splitByPackage: {}, sourceDirs: {}",
            input,
            output,
            splitByPackage,
            sourceDirectories.joinToString("\n", "\n")
        )
        val j2c = J2CJackson()
        val jacocoData = j2c.loadJacocoData(input)

        if (splitByPackage) {
            // splitOutputDirectory cannot be queried here: its convention maps this task's own
            // outputFile, and Gradle only resolves that after the task completes
            val splitDir = splitDirFor(output).also { it.mkdirs() }
            val packageNames = mutableSetOf<String>()
            jacocoData.packages.forEach { packageElement ->
                val packageName = packageElement.name?.replace('/', '.') ?: ""
                packageNames += packageName
                val packageData = jacocoData.copy(packagesReport = listOf(packageElement))
                val packageOut = File(splitDir, "${packageName}.xml")
                j2c.writeCoberturaData(packageOut, j2c.transformData(packageData, sourceDirectories, rootPackage))
                logger.lifecycle(
                    "Cobertura report for package $packageName generated at ${packageOut.absolutePath}"
                )
            }
            // Gradle does not prune untracked files inside an @OutputDirectory, so a package
            // that disappeared from the report would otherwise leave a stale report behind
            splitDir.listFiles { file -> file.isFile && file.name.endsWith(".xml") && file.name.removeSuffix(".xml") !in packageNames }
                ?.forEach { it.delete() }
            j2c.writeCoberturaData(output, j2c.transformData(jacocoData, sourceDirectories, rootPackage))
            logger.lifecycle("Cobertura report generated at ${output.absolutePath}")
        } else {
            j2c.writeCoberturaData(output, j2c.transformData(jacocoData, sourceDirectories, rootPackage))
            logger.lifecycle("Cobertura report generated at ${output.absolutePath}")
        }
    }
}

class JacocoToCoberturaException(msg: String) : Exception(msg)
