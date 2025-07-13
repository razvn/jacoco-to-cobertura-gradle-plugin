package net.razvan

import org.gradle.api.plugins.JavaPlugin
import org.gradle.kotlin.dsl.apply
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testing.jacoco.plugins.JacocoPlugin
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class JacocoToCoberturaPluginTest {
    @Test
    fun `task exists`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply(JacocoToCoberturaPlugin::class)

        assertDoesNotThrow {
            project.tasks.getByName(JacocoToCoberturaPlugin.TASK_NAME)
        }
    }

    @Test
    fun `plugin creates task with correct name`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply(JacocoToCoberturaPlugin::class)

        val task = project.tasks.getByName(JacocoToCoberturaPlugin.TASK_NAME)
        assertEquals("jacocoToCobertura", task.name)
        assertTrue(task is JacocoToCoberturaTask)
    }

    @Test
    fun `plugin configures task when JaCoCo plugin is applied`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply(JavaPlugin::class)
        project.plugins.apply(JacocoPlugin::class)
        project.plugins.apply(JacocoToCoberturaPlugin::class)

        val task = project.tasks.getByName(JacocoToCoberturaPlugin.TASK_NAME) as JacocoToCoberturaTask
        val jacocoTask = project.tasks.getByName("jacocoTestReport") as JacocoReport

        // Check that dependencies are set up correctly
        assertTrue(task.dependsOn.contains(jacocoTask))
        
        // Check that default values are configured
        assertFalse(task.splitByPackage.get())
        assertEquals("", task.rootPackageToRemove.get())
    }

    @Test
    fun `plugin configures input and output files when JaCoCo plugin is applied`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply(JavaPlugin::class)
        project.plugins.apply(JacocoPlugin::class)
        project.plugins.apply(JacocoToCoberturaPlugin::class)

        val task = project.tasks.getByName(JacocoToCoberturaPlugin.TASK_NAME) as JacocoToCoberturaTask
        val jacocoTask = project.tasks.getByName("jacocoTestReport") as JacocoReport

        // Input should be configured from JaCoCo task
        val jacocoXmlReport = jacocoTask.reports.xml
        if (jacocoXmlReport.required.get()) {
            assertEquals(jacocoXmlReport.outputLocation.get().asFile, task.inputFile.get().asFile)
        }

        // Output should be in build directory
        assertTrue(task.outputFile.get().asFile.absolutePath.contains("build"))
        assertTrue(task.outputFile.get().asFile.name.endsWith(".xml"))
    }

    @Test
    fun `plugin works without JaCoCo plugin`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply(JacocoToCoberturaPlugin::class)

        val task = project.tasks.getByName(JacocoToCoberturaPlugin.TASK_NAME) as JacocoToCoberturaTask

        // Task should exist but not have JaCoCo dependencies
        assertFalse(task.dependsOn.any { it.toString().contains("jacoco") })
        
        // Default values should still be set
        assertFalse(task.splitByPackage.get())
        assertEquals("", task.rootPackageToRemove.get())
    }

    @Test
    fun `task has correct default configuration`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply(JacocoToCoberturaPlugin::class)

        val task = project.tasks.getByName(JacocoToCoberturaPlugin.TASK_NAME) as JacocoToCoberturaTask

        // Check default values set by convention
        assertFalse(task.splitByPackage.get())
        assertEquals("", task.rootPackageToRemove.get())
        
        // Source directories should be empty by default
        assertTrue(task.sourceDirectories.isEmpty)
    }

    @Test
    fun `plugin configures source directories from JaCoCo plugin`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply(JavaPlugin::class)
        project.plugins.apply(JacocoPlugin::class)
        project.plugins.apply(JacocoToCoberturaPlugin::class)

        val task = project.tasks.getByName(JacocoToCoberturaPlugin.TASK_NAME) as JacocoToCoberturaTask

        // Source directories should be configured from Java plugin
        assertFalse(task.sourceDirectories.isEmpty)
    }
}
