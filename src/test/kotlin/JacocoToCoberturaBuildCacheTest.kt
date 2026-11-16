package net.razvan

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class JacocoToCoberturaBuildCacheTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `split reports use the source paths from the current checkout`() {
        val first = createCheckout("first")
        val second = createCheckout("second")

        assertEquals(TaskOutcome.SUCCESS, convert(first))
        assertEquals(TaskOutcome.SUCCESS, convert(second))
        assertReportSource(second)

        second.resolve("build").deleteRecursively()
        assertEquals(TaskOutcome.FROM_CACHE, convert(second))
        assertReportSource(second)
    }

    private fun createCheckout(name: String): File {
        val checkout = tempDir.resolve(name)
        val sourceDir = checkout.resolve("src/main/java").apply { mkdirs() }
        sourceDir.resolve("Example.java").writeText("package example; class Example {}")
        checkout.resolve("jacoco.xml").writeText("""<report name="example"><package name="example"/></report>""")
        checkout.resolve("settings.gradle").writeText("""
            rootProject.name = 'example'
            buildCache {
                local { directory = new File(settingsDir, '../cache') }
            }
        """.trimIndent())
        checkout.resolve("build.gradle").writeText("""
            plugins { id 'net.razvan.jacoco-to-cobertura' }
            jacocoToCobertura {
                inputFile.set(layout.projectDirectory.file('jacoco.xml'))
                outputFile.set(layout.buildDirectory.file('reports/cobertura.xml'))
                sourceDirectories.from(layout.projectDirectory.dir('src/main/java'))
                splitByPackage.set(true)
            }
        """.trimIndent())
        return checkout
    }

    private fun convert(checkout: File): TaskOutcome? =
        GradleRunner.create()
            .withProjectDir(checkout)
            .withTestKitDir(tempDir.resolve("testkit"))
            .withPluginClasspath()
            .withArguments("jacocoToCobertura", "--build-cache", "--offline")
            .build()
            .task(":jacocoToCobertura")?.outcome

    private fun assertReportSource(checkout: File) {
        val report = checkout.resolve("build/reports/cobertura-packages/example.xml")
        val xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(report)
        val sourcePath = File(xml.getElementsByTagName("source").item(0).textContent)
        assertEquals(checkout.resolve("src/main/java").canonicalFile, sourcePath.canonicalFile)
    }
}
