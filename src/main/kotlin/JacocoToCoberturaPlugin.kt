package net.razvan

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
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
        println("Conversion jacoco report to cobertura")
        try {
            val input = extension.inputFile.get().asFile
                    .also {
                        if (!it.exists()) throw JacocoToCoberturaException("File `${it.canonicalPath}` does not exists (current dir: `${File(".").canonicalPath})`")
                    }
            val output = extension.outputFile.getOrNull()?.asFile ?: defaultOutputFile(input)

            val jacocoData = loadJacocoData(input)
            val roots = sourcesRoots(
                    project.extensions.getByType(JavaPluginExtension::class.java).sourceSets,
                    jacocoData
            )

            writeCoberturaData(output, transformData(jacocoData, roots))
            println("Cobertura report generated in: `$output`")
        } catch (e: Exception) {
            println("Error while running JacocoToCobertura conversion: `${e.message}")
            if (e !is JacocoToCoberturaException) {
                throw e
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

    private fun sourcesRoots(sourcesSet: SourceSetContainer?, jacocoData: Jacoco.Report): Set<String> {
        val allSources = sourcesSet?.getByName(SourceSet.MAIN_SOURCE_SET_NAME)?.allSource
                ?: emptyList()
        val jacocoPackages = jacocoData.packagesNames()

        return allSources.map { it.canonicalPath }
                .mapNotNull { sourceName ->
                    val p = jacocoPackages.firstOrNull { sourceName.contains(it) }
                    p?.let { sourceName.substringBefore(it) }
                }.toSet()
    }
}

interface JacocoToCoberturaExtension {
    @get:InputFile
    val inputFile: RegularFileProperty

    @get:OutputFile
    val outputFile: RegularFileProperty
}

private class JacocoToCoberturaException(msg: String) : Exception(msg)
