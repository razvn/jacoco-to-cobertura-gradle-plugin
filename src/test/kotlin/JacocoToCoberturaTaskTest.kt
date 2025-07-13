package net.razvan

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File

class JacocoToCoberturaTaskTest {

    private lateinit var project: Project
    private lateinit var task: JacocoToCoberturaTask

    @TempDir
    lateinit var tempDir: File

    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder().build()
        val taskProvider = project.tasks.register("testJacocoToCobertura", JacocoToCoberturaTask::class.java) {
            // Set required default values
            splitByPackage.set(false)
            rootPackageToRemove.set("")
        }
        task = taskProvider.get()
    }

    @Test
    fun `task throws exception for non-existent input file`() {
        val nonExistentFile = File(tempDir, "non-existent.xml")
        val outputFile = File(tempDir, "output.xml")

        task.inputFile.set(nonExistentFile)
        task.outputFile.set(outputFile)
        task.sourceDirectories.setFrom(emptyList<File>())
        task.splitByPackage.set(false)
        task.rootPackageToRemove.set("")

        val exception = assertThrows<JacocoToCoberturaException> {
            task.convert()
        }

        assertTrue(exception.message!!.contains("does not exist"))
        assertTrue(exception.message!!.contains(nonExistentFile.absolutePath))
    }

    @Test
    fun `task creates parent directory when missing`() {
        val inputFile = File(tempDir, "input.xml")
        inputFile.writeText("""
            <?xml version="1.0" encoding="UTF-8"?>
            <report name="test">
                <sessioninfo id="session" start="1000000" dump="1000001"/>
                <counter type="LINE" missed="0" covered="0"/>
            </report>
        """.trimIndent())

        val nestedDir = File(tempDir, "nested/deep/directory")
        val outputFile = File(nestedDir, "output.xml")

        task.inputFile.set(inputFile)
        task.outputFile.set(outputFile)
        task.sourceDirectories.setFrom(emptyList<File>())
        task.splitByPackage.set(false)
        task.rootPackageToRemove.set("")

        // Parent directory should not exist initially
        assertFalse(nestedDir.exists())

        task.convert()

        // Parent directory should be created
        assertTrue(nestedDir.exists())
        assertTrue(outputFile.exists())
    }

    @Test
    fun `task handles parent directory creation failure`() {
        val inputFile = File(tempDir, "input.xml")
        inputFile.writeText("""
            <?xml version="1.0" encoding="UTF-8"?>
            <report name="test">
                <counter type="LINE" missed="0" covered="0"/>
            </report>
        """.trimIndent())

        // Create a file where we want to put a directory (this will cause mkdirs to fail)
        val conflictingFile = File(tempDir, "conflicting")
        conflictingFile.writeText("blocking")
        val outputFile = File(conflictingFile, "output.xml") // This should fail

        task.inputFile.set(inputFile)
        task.outputFile.set(outputFile)
        task.sourceDirectories.setFrom(emptyList<File>())
        task.splitByPackage.set(false)
        task.rootPackageToRemove.set("")

        val exception = assertThrows<JacocoToCoberturaException> {
            task.convert()
        }

        // The exception should contain information about directory creation failure
        assertTrue(exception.message!!.contains("Parent path") || exception.message!!.contains("Output file directory") || exception.message!!.contains("Not a directory"))
    }

    @Test
    fun `task processes valid JaCoCo file successfully`() {
        val inputFile = File(tempDir, "input.xml")
        inputFile.writeText("""
            <?xml version="1.0" encoding="UTF-8"?>
            <report name="test">
                <sessioninfo id="session" start="1000000" dump="1000001"/>
                <package name="com/example">
                    <class name="com/example/TestClass" sourcefilename="TestClass.java">
                        <method name="testMethod" desc="()V" line="10">
                            <counter type="INSTRUCTION" missed="0" covered="5"/>
                            <counter type="LINE" missed="0" covered="1"/>
                        </method>
                        <counter type="INSTRUCTION" missed="0" covered="5"/>
                        <counter type="LINE" missed="0" covered="1"/>
                    </class>
                    <sourcefile name="TestClass.java">
                        <line nr="10" mi="0" ci="5" mb="0" cb="0"/>
                    </sourcefile>
                    <counter type="INSTRUCTION" missed="0" covered="5"/>
                    <counter type="LINE" missed="0" covered="1"/>
                </package>
                <counter type="INSTRUCTION" missed="0" covered="5"/>
                <counter type="LINE" missed="0" covered="1"/>
            </report>
        """.trimIndent())

        val outputFile = File(tempDir, "output.xml")

        task.inputFile.set(inputFile)
        task.outputFile.set(outputFile)
        task.sourceDirectories.setFrom(listOf(File("src/main/java")))
        task.splitByPackage.set(false)
        task.rootPackageToRemove.set("")

        task.convert()

        assertTrue(outputFile.exists())
        val outputContent = outputFile.readText()
        assertTrue(outputContent.contains("<?xml"))
        assertTrue(outputContent.contains("<coverage"))
        assertTrue(outputContent.contains("com.example"))
    }

    @Test
    fun `task handles splitByPackage configuration`() {
        val inputFile = File(tempDir, "input.xml")
        inputFile.writeText("""
            <?xml version="1.0" encoding="UTF-8"?>
            <report name="test">
                <sessioninfo id="session" start="1000000" dump="1000001"/>
                <package name="com/example/package1">
                    <counter type="LINE" missed="0" covered="1"/>
                </package>
                <package name="com/example/package2">
                    <counter type="LINE" missed="0" covered="1"/>
                </package>
                <counter type="LINE" missed="0" covered="2"/>
            </report>
        """.trimIndent())

        val outputFile = File(tempDir, "output.xml")

        task.inputFile.set(inputFile)
        task.outputFile.set(outputFile)
        task.sourceDirectories.setFrom(emptyList<File>())
        task.splitByPackage.set(true)
        task.rootPackageToRemove.set("")

        task.convert()

        // When splitByPackage is true, multiple files should be created
        val package1File = File(tempDir, "output-com.example.package1.xml")
        val package2File = File(tempDir, "output-com.example.package2.xml")

        assertTrue(package1File.exists())
        assertTrue(package2File.exists())
    }

    @Test
    fun `task handles rootPackageToRemove configuration`() {
        val inputFile = File(tempDir, "input.xml")
        inputFile.writeText("""
            <?xml version="1.0" encoding="UTF-8"?>
            <report name="test">
                <sessioninfo id="session" start="1000000" dump="1000001"/>
                <package name="com/example/myproject/core">
                    <class name="com/example/myproject/core/TestClass" sourcefilename="TestClass.java">
                        <counter type="LINE" missed="0" covered="1"/>
                    </class>
                    <counter type="LINE" missed="0" covered="1"/>
                </package>
                <counter type="LINE" missed="0" covered="1"/>
            </report>
        """.trimIndent())

        val outputFile = File(tempDir, "output.xml")

        task.inputFile.set(inputFile)
        task.outputFile.set(outputFile)
        task.sourceDirectories.setFrom(emptyList<File>())
        task.splitByPackage.set(false)
        task.rootPackageToRemove.set("com.example.myproject")

        task.convert()

        assertTrue(outputFile.exists())
        val outputContent = outputFile.readText()
        
        // The root package should be removed from the output
        assertTrue(outputContent.contains("core/TestClass"))
        assertFalse(outputContent.contains("com/example/myproject/core"))
    }

    @Test
    fun `task handles empty rootPackageToRemove gracefully`() {
        val inputFile = File(tempDir, "input.xml")
        inputFile.writeText("""
            <?xml version="1.0" encoding="UTF-8"?>
            <report name="test">
                <sessioninfo id="session" start="1000000" dump="1000001"/>
                <package name="com/example">
                    <counter type="LINE" missed="0" covered="1"/>
                </package>
                <counter type="LINE" missed="0" covered="1"/>
            </report>
        """.trimIndent())

        val outputFile = File(tempDir, "output.xml")

        task.inputFile.set(inputFile)
        task.outputFile.set(outputFile)
        task.sourceDirectories.setFrom(emptyList<File>())
        task.splitByPackage.set(false)
        task.rootPackageToRemove.set("   ")  // Whitespace-only string

        task.convert()

        assertTrue(outputFile.exists())
        // Should work without issues even with whitespace-only rootPackageToRemove
    }

    @Test
    fun `task configuration properties have correct defaults`() {
        assertEquals(false, task.splitByPackage.get())
        assertEquals("", task.rootPackageToRemove.get())
        assertTrue(task.sourceDirectories.isEmpty)
    }

    @Test
    fun `task properties can be configured`() {
        val inputFile = File(tempDir, "input.xml")
        val outputFile = File(tempDir, "output.xml")
        val sourceDir = File("src/main/java")

        task.inputFile.set(inputFile)
        task.outputFile.set(outputFile)
        task.sourceDirectories.setFrom(listOf(sourceDir))
        task.splitByPackage.set(true)
        task.rootPackageToRemove.set("com.example")

        assertEquals(inputFile, task.inputFile.get().asFile)
        assertEquals(outputFile, task.outputFile.get().asFile)
        assertTrue(task.sourceDirectories.files.first().absolutePath.endsWith("src/main/java"))
        assertTrue(task.splitByPackage.get())
        assertEquals("com.example", task.rootPackageToRemove.get())
    }
}
