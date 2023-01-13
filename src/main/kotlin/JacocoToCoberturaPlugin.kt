package net.razvan

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.simpleframework.xml.Serializer
import org.simpleframework.xml.core.Persister
import org.simpleframework.xml.stream.Format
import java.io.File

class JacocoToCoberturaPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.task("jacocoToCobertura") { task ->
            val extension = project.extensions.create("jacocoToCobertura", JacocoToCoberturaExtension::class.java)
            task.doLast {
                convert(project, extension)
            }
        }.apply {
            group = "verification"
        }
    }

    private fun convert(project: Project, extension: JacocoToCoberturaExtension) {
        log(extension, "Conversion jacoco report to cobertura")
        try {
            val input = extension.inputFile.get().asFile
                .also {
                    if (!it.exists()) throw JacocoToCoberturaException(
                        "File `${it.canonicalPath}` does not exists (current dir: `${
                            File(
                                "."
                            ).canonicalPath
                        })`"
                    )
                }
            val output = extension.outputFile.orNull?.asFile ?: defaultOutputFile(input)
            makeDirsIfNeeded(output.parentFile)

            val splitByPackage = extension.splitByPackage.getOrElse(false)

            log(
                extension,
                "\tJacocoToCobertura: Calculated configuration: input: $input, output: $output, splitByPackage: $splitByPackage"
            )
            val customSourcesConf = emptySet<File>()

            val jacocoData = loadJacocoData(input)

            val kotlinSources: Set<File> = allKotlinSources(project)

            val javaSources = runCatching { project.extensions.getByType(JavaPluginExtension::class.java).sourceSets }
                .getOrNull()
                ?.findByName(SourceSet.MAIN_SOURCE_SET_NAME)?.allSource?.files
                ?.filter { it !in kotlinSources }
                ?: emptySet()
            val customSources = customSourcesConf
                .filter { it.exists() }
                .mapNotNull { it.listFiles()?.toList() }
                .flatten()
                .toSet()

            log(extension, "Sources: - Kotlin: `${kotlinSources.size}`, Java: `${javaSources.size}`")
            val allSources = kotlinSources + javaSources + customSources
            val roots = sourcesRoots(allSources, jacocoData)

            if (splitByPackage) {
                jacocoData.packages.forEach { packageElement ->
                    val packageName = packageElement.name?.replace('/', '.')
                    val packageData = jacocoData.copy(packages = listOf(packageElement))
                    val packageOut = File(output.absolutePath.replace(".xml", "-${packageName}.xml"))
                    writeCoberturaData(packageOut, transformData(packageData, roots))
                }
            } else {
                writeCoberturaData(output, transformData(jacocoData, roots))
            }

            log(extension, "Cobertura report generated in: `$output`")
        } catch (e: Exception) {
            println("Error while running JacocoToCobertura conversion: `${e.message} [configuration: input: `${extension.inputFile.get()}` | output: `${extension.outputFile.get()}`]")
            if (e !is JacocoToCoberturaException) {
                throw e
            }
        }
    }

    private fun allKotlinSources(project: Project): Set<File> {
        val multi = runCatching {
            kotlinSourcesFiles(project.extensions.getByType(KotlinMultiplatformExtension::class.java).sourceSets)
        }.getOrDefault(emptySet())
        val android = runCatching {
            kotlinSourcesFiles(project.extensions.getByType(KotlinAndroidProjectExtension::class.java).sourceSets)
        }.getOrDefault(emptySet())
        val jvm = runCatching {
            kotlinSourcesFiles(project.extensions.getByType(KotlinJvmProjectExtension::class.java).sourceSets)
        }.getOrDefault(emptySet())
        val js = runCatching {
            kotlinSourcesFiles(project.extensions.getByType(KotlinJsProjectExtension::class.java).sourceSets)
        }.getOrDefault(emptySet())

        return multi + android + jvm + js
    }

    private fun kotlinSourcesFiles(sourceSets: NamedDomainObjectContainer<KotlinSourceSet>): Set<File> =
        sourceSets
            .filterNot { it.name.contains("test", true) }
            .flatMap { sourcesSet ->
                sourcesSet.kotlin.srcDirs.flatMap { file ->
                    file.walkTopDown().mapNotNull {
                        if (it.isFile) it else null
                    }
                }
            }.toSet()

    @Throws(JacocoToCoberturaException::class)
    private fun makeDirsIfNeeded(file: File?) {
        if (file != null && !file.exists()) {
            try {
                if (!file.mkdirs()) throw JacocoToCoberturaException("`mkdirs()` returned false")
            } catch (e: Exception) {
                throw JacocoToCoberturaException("Output file directory `${file.canonicalPath} does not exists and couldn't be created, error: `${e.message}`")
            }
        }
    }

    @Throws(JacocoToCoberturaException::class)
    private fun transformData(jacocoData: Jacoco.Report, sources: Collection<String>) = try {
        Cobertura.Coverage(jacocoData, sources)
    } catch (e: Exception) {
        throw JacocoToCoberturaException("Transforming Jacoco Data to Cobertura error: `${e.message}`")
    }

    @Throws(JacocoToCoberturaException::class)
    private fun writeCoberturaData(outputFile: File, data: Cobertura.Coverage) = with(outputFile) {
        try {
            Persister(Format("<?xml version=\"1.0\" encoding= \"UTF-8\" ?>")).write(data, this)
        } catch (e: Exception) {
            throw JacocoToCoberturaException("Writing Cobertura Data to file `${this.canonicalPath}` error: `${e.message}`")
        }
    }

    @Throws(JacocoToCoberturaException::class)
    private fun loadJacocoData(fileIn: File): Jacoco.Report = try {
        val serializer: Serializer = Persister()
        serializer.read(Jacoco.Report::class.java, fileIn)
    } catch (e: Exception) {
        throw JacocoToCoberturaException("Loading Jacoco report error: `${e.message}`")
    }

    private fun defaultOutputFile(inputFile: File): File {
        val inputPath = inputFile.canonicalPath
        val position = inputPath.lastIndexOf("/") + 1
        val outputPath = "${inputPath.substring(0, position)}cobertura-${inputPath.substring(position)}"
        val outputFile = File(outputPath)
        if (!outputFile.canWrite()) throw JacocoToCoberturaException("Can't write to the default location of: `$outputPath`")
        return outputFile
    }

    private fun sourcesRoots(allSources: Set<File>, jacocoData: Jacoco.Report): Set<String> {
        val jacocoPackages = jacocoData.packagesNames()
        return allSources.map { it.canonicalPath }
            .mapNotNull { sourceName ->
                val p = jacocoPackages.firstOrNull { sourceName.contains(it) }
                    ?: jacocoPackages.firstOrNull {
                        sourceName.contains(
                            it.replace(
                                "/",
                                "."
                            )
                        )
                    } // in case the package is in dot format
                p?.let {
                    val sourcePath = sourceName.substringBefore(it)
                    if (sourcePath != sourceName) sourcePath else sourceName.substringBefore(it.replace("/", "."))
                }
            }.toSet()
    }

    fun log(extension: JacocoToCoberturaExtension, msg: String) {
        if (extension.verbose.getOrElse(false)) println(msg)
    }
}

interface JacocoToCoberturaExtension {
    @get:InputFile
    val inputFile: RegularFileProperty

    @get:OutputFile
    val outputFile: RegularFileProperty

    @get:Input
    val splitByPackage: Property<Boolean>

    @get:Input
    val verbose: Property<Boolean>
}

private class JacocoToCoberturaException(msg: String) : Exception(msg)
