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
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
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

    // Cobertura embeds these absolute paths even when source files have identical relative fingerprints.
    @get:Input
    val sourcePaths: List<String>
        get() = sourceDirectories.files.map { it.absolutePath }

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

        val sourcePaths = sourcePaths
        val splitByPackage = splitByPackage.getOrElse(false)
        val rootPackage = rootPackageToRemove.getOrElse("").takeIf { it.isNotBlank() }

        logger.debug(
            "Calculated configuration: input: {}, output: {}, splitByPackage: {}, sourceDirs: {}",
            input,
            output,
            splitByPackage,
            sourcePaths.joinToString("\n", "\n")
        )
        val j2c = J2CJackson()
        val jacocoData = j2c.loadJacocoData(input)

        if (splitByPackage) {
            val splitDir = splitOutputDirectory.get().asFile
            Files.createDirectories(splitDir.toPath())
            val manifest = splitDir.resolve(".cobertura-generated-files")
            val reservedPaths = setOf(input.canonicalFile, output.canonicalFile)
            if (manifest.canonicalFile in reservedPaths) {
                throw JacocoToCoberturaException("Split report manifest conflicts with an input or output file: ${manifest.absolutePath}")
            }
            val previousNames = readSplitManifest(manifest)
            val reports = jacocoData.packages.map { packageElement ->
                val packageName = packageElement.name?.replace('/', '.')?.takeIf { it.isNotBlank() }
                // '[' is forbidden in JVM package names, so this filename is reserved for the unnamed package.
                val unnamedPackageFile = "[default].xml"
                val fileName = packageName?.let { "$it.xml" } ?: unnamedPackageFile
                if (!isSplitReportName(fileName) ||
                    (packageName != null && fileName.equals(unnamedPackageFile, ignoreCase = true))
                ) {
                    throw JacocoToCoberturaException("Invalid split report filename: $fileName")
                }
                val file = splitDir.resolve(fileName)
                val path = file.toPath()
                if (file.canonicalFile in reservedPaths ||
                    (Files.exists(path, NOFOLLOW_LINKS) &&
                        (fileName !in previousNames || !Files.isRegularFile(path, NOFOLLOW_LINKS)))
                ) {
                    throw JacocoToCoberturaException("Split report conflicts with an existing file or configured path: ${file.absolutePath}")
                }
                file to packageElement
            }
            val currentNames = reports.map { (file, _) -> file.name }.toSet()

            // Record new destinations before writing so a failed conversion can retry its partial outputs.
            if (!Files.exists(manifest.toPath()) || !previousNames.containsAll(currentNames)) {
                writeSplitManifest(manifest, previousNames + currentNames)
            }
            reports.forEach { (packageOut, packageElement) ->
                val packageData = jacocoData.copy(packagesReport = listOf(packageElement))
                j2c.writeCoberturaData(packageOut, j2c.transformData(packageData, sourcePaths, rootPackage))
                logger.lifecycle(
                    "Cobertura report for package ${packageOut.nameWithoutExtension} generated at ${packageOut.absolutePath}"
                )
            }

            j2c.writeCoberturaData(output, j2c.transformData(jacocoData, sourcePaths, rootPackage))
            val staleNames = previousNames - currentNames
            staleNames.forEach { name ->
                val file = splitDir.resolve(name)
                if (file.canonicalFile !in reservedPaths && Files.isRegularFile(file.toPath(), NOFOLLOW_LINKS)) {
                    Files.deleteIfExists(file.toPath())
                }
            }
            if (staleNames.isNotEmpty()) {
                writeSplitManifest(manifest, currentNames)
            }
        } else {
            j2c.writeCoberturaData(output, j2c.transformData(jacocoData, sourcePaths, rootPackage))
        }
        logger.lifecycle("Cobertura report generated at ${output.absolutePath}")
    }

    private fun isSplitReportName(name: String): Boolean =
        name.length > 4 && name.endsWith(".xml") &&
            name.none { it == '/' || it == '\\' || it == ':' || it.isISOControl() }

    private fun readSplitManifest(manifest: File): Set<String> {
        val path = manifest.toPath()
        if (!Files.exists(path, NOFOLLOW_LINKS)) return emptySet()
        if (!Files.isRegularFile(path, NOFOLLOW_LINKS)) {
            throw JacocoToCoberturaException("Split report manifest is not a regular file: ${manifest.absolutePath}")
        }
        val names = manifest.readLines().toSet()
        if (!names.all(::isSplitReportName)) {
            throw JacocoToCoberturaException("Invalid filename in split report manifest: ${manifest.absolutePath}")
        }
        return names
    }

    private fun writeSplitManifest(manifest: File, names: Set<String>) {
        val temp = Files.createTempFile(manifest.parentFile.toPath(), manifest.name, ".tmp")
        try {
            Files.write(temp, names.sorted())
            Files.move(temp, manifest.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(temp)
        }
    }
}

class JacocoToCoberturaException(msg: String) : Exception(msg)
