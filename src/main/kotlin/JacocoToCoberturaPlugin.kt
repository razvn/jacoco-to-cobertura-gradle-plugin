package net.razvan

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
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
        val input = extension.inputFile.getOrElse("./build/reports/xml/coverage.xml")
        val output = extension.outputFile.getOrElse(addDefaultPrefixToFile(input))
        try {
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
    private fun writeCoberturaData(outputFile: String, data: Cobertura.Coverage) = with(File(outputFile)) {
        try {
            Persister(Format("<?xml version=\"1.0\" encoding= \"UTF-8\" ?>")).write(data, this)
        } catch (e: Exception) {
            throw JacocoToCoberturaException("Writing Cobertura Data to file `${this.canonicalPath}` error: `${e.message}`")
        }
    }

    @Throws(JacocoToCoberturaException::class)
    private fun loadJacocoData(inputFile: String): Jacoco.Report = try {
        val fileIn = File(inputFile)
        if (!fileIn.exists()) throw JacocoToCoberturaException("File `${fileIn.canonicalPath}` does not exists (current dir: `${File(".").canonicalPath})`")
        val serializer: Serializer = Persister()
        serializer.read(Jacoco.Report::class.java, fileIn)
    } catch (e: Exception) {
        throw JacocoToCoberturaException("Loading Jacoco report error: `${e.message}`")
    }

    private fun addDefaultPrefixToFile(inputFile: String): String {
        val position = inputFile.lastIndexOf("/")
        return "${inputFile.substring(0, position)}cobertura-${inputFile.substring(position)}"
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
    val inputFile: Property<String>
    val outputFile: Property<String>
}

private class JacocoToCoberturaException(msg: String) : Exception(msg)
