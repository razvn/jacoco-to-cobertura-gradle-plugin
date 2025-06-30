package net.razvan.models

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CoberturaModelsTest {

    @Test
    fun `Coverage constructor handles empty sources collection`() {
        val report = JacocoModels.Report()
        val coverage = CoberturaModels.Coverage(report, emptyList())
        
        assertEquals(1, coverage.sources.size)
        assertEquals(".", coverage.sources[0].value)
    }

    @Test
    fun `Coverage constructor handles null rootPackageToRemove`() {
        val report = JacocoModels.Report()
        val coverage = CoberturaModels.Coverage(report, listOf("src/main/java"), null)
        
        assertEquals(1, coverage.sources.size)
        assertEquals("src/main/java", coverage.sources[0].value)
    }

    @Test
    fun `Coverage constructor uses provided sources when not empty`() {
        val report = JacocoModels.Report()
        val sources = listOf("src/main/java", "src/test/java")
        val coverage = CoberturaModels.Coverage(report, sources)
        
        assertEquals(2, coverage.sources.size)
        assertEquals("src/main/java", coverage.sources[0].value)
        assertEquals("src/test/java", coverage.sources[1].value)
    }

    @Test
    fun `Package constructor handles null package name`() {
        val packageElement = JacocoModels.PackageElement().apply {
            name = null
            counters = emptyList()
        }
        
        val coberturaPackage = CoberturaModels.Package(packageElement)
        
        assertEquals("", coberturaPackage.name)
    }

    @Test
    fun `Package constructor transforms slash to dot in name`() {
        val packageElement = JacocoModels.PackageElement().apply {
            name = "com/example/test"
            counters = emptyList()
        }
        
        val coberturaPackage = CoberturaModels.Package(packageElement)
        
        assertEquals("com.example.test", coberturaPackage.name)
    }

    @Test
    fun `ClassElement cleanPackageName handles null package name`() {
        val classElement = JacocoModels.ClassElement().apply {
            name = "TestClass"
            counters = emptyList()
        }
        val packageElement = JacocoModels.PackageElement().apply {
            name = null
            sourcefiles = emptyList()
        }
        
        val coberturaClass = CoberturaModels.ClassElement(classElement, packageElement)
        
        // Should handle null package name gracefully
        assertTrue(coberturaClass.filename.endsWith("TestClass"))
    }

    @Test
    fun `ClassElement cleanPackageName handles empty rootPackageToRemove`() {
        val classElement = JacocoModels.ClassElement().apply {
            name = "com/example/TestClass"
            counters = emptyList()
        }
        val packageElement = JacocoModels.PackageElement().apply {
            name = "com/example"
            sourcefiles = emptyList()
        }
        
        val coberturaClass = CoberturaModels.ClassElement(classElement, packageElement, "")
        
        assertTrue(coberturaClass.filename.contains("com/example/"))
    }

    @Test
    fun `ClassElement cleanPackageName removes root package prefix`() {
        val classElement = JacocoModels.ClassElement().apply {
            name = "TestClass"
            counters = emptyList()
        }
        val packageElement = JacocoModels.PackageElement().apply {
            name = "com.example.project"
            sourcefiles = emptyList()
        }
        
        val coberturaClass = CoberturaModels.ClassElement(classElement, packageElement, "com.example")
        
        assertTrue(coberturaClass.filename.contains("project/"))
        assertFalse(coberturaClass.filename.contains("com/example/"))
    }

    @Test
    fun `ClassElement getSourceName handles null sourceFileName and name`() {
        val classElement = JacocoModels.ClassElement().apply {
            name = null
            sourcefilename = null
            counters = emptyList()
        }
        val packageElement = JacocoModels.PackageElement().apply {
            name = "com/example"
            sourcefiles = emptyList()
        }
        
        val coberturaClass = CoberturaModels.ClassElement(classElement, packageElement)
        
        // Should handle null values gracefully
        assertTrue(coberturaClass.filename.isNotEmpty())
    }

    @Test
    fun `ClassElement getSourceName finds matching source file`() {
        val sourceFile = JacocoModels.SourceFile().apply {
            name = "TestClass.java"
        }
        val classElement = JacocoModels.ClassElement().apply {
            name = "com/example/TestClass"
            sourcefilename = "TestClass"
            counters = emptyList()
        }
        val packageElement = JacocoModels.PackageElement().apply {
            name = "com/example"
            sourcefiles = listOf(sourceFile)
        }
        
        val coberturaClass = CoberturaModels.ClassElement(classElement, packageElement)
        
        assertTrue(coberturaClass.filename.endsWith("TestClass.java"))
    }

    @Test
    fun `ClassElement getSourceName falls back to source name when no match`() {
        val classElement = JacocoModels.ClassElement().apply {
            name = "com/example/TestClass"
            sourcefilename = "TestClass"
            counters = emptyList()
        }
        val packageElement = JacocoModels.PackageElement().apply {
            name = "com/example"
            sourcefiles = emptyList()
        }
        
        val coberturaClass = CoberturaModels.ClassElement(classElement, packageElement)
        
        assertTrue(coberturaClass.filename.endsWith("TestClass"))
    }

    @Test
    fun `linesForMethod returns empty for null method line`() {
        val method = JacocoModels.MethodElement().apply { 
            line = null
            name = "testMethod"
        }
        val packageElement = JacocoModels.PackageElement()
        
        val result = CoberturaModels.linesForMethod(method, packageElement, "Test.java", "Test")
        
        assertTrue(result.isEmpty())
    }

    @Test
    fun `linesForMethod returns empty when both jSource and jName are null`() {
        val method = JacocoModels.MethodElement().apply { 
            line = 10
            name = "testMethod"
        }
        val packageElement = JacocoModels.PackageElement()
        
        val result = CoberturaModels.linesForMethod(method, packageElement, null, null)
        
        assertTrue(result.isEmpty())
    }

    @Test
    fun `linesForMethod filters source files by name correctly`() {
        val sourceFile1 = JacocoModels.SourceFile().apply {
            name = "Test.java"
            lines = listOf(
                JacocoModels.Line().apply { nr = 15 },
                JacocoModels.Line().apply { nr = 20 }
            )
        }
        val sourceFile2 = JacocoModels.SourceFile().apply {
            name = "Other.java"
            lines = listOf(
                JacocoModels.Line().apply { nr = 25 }
            )
        }
        
        val method = JacocoModels.MethodElement().apply { 
            line = 10
            name = "testMethod"
        }
        val packageElement = JacocoModels.PackageElement().apply {
            sourcefiles = listOf(sourceFile1, sourceFile2)
        }
        
        val result = CoberturaModels.linesForMethod(method, packageElement, "Test.java", "Test")
        
        assertEquals(2, result.size)
        assertTrue(result.all { it.nr >= 10 })
        assertTrue(result.any { it.nr == 15 })
        assertTrue(result.any { it.nr == 20 })
    }

    @Test
    fun `Line constructor handles branch detection correctly`() {
        val jacocoLine = JacocoModels.Line().apply {
            nr = 10
            ci = 1
            mb = 2  // missed branches
            cb = 3  // covered branches
        }
        
        val coberturaLine = CoberturaModels.Line(jacocoLine)
        
        assertTrue(coberturaLine.branch)
        assertEquals("60% (3/5)", coberturaLine.conditionCoverage)
        assertEquals(1, coberturaLine.conditions?.size)
        assertEquals("60%", coberturaLine.conditions?.get(0)?.coverage)
    }

    @Test
    fun `Line constructor handles no branches correctly`() {
        val jacocoLine = JacocoModels.Line().apply {
            nr = 10
            ci = 1
            mb = 0  // no missed branches
            cb = 0  // no covered branches
        }
        
        val coberturaLine = CoberturaModels.Line(jacocoLine)
        
        assertFalse(coberturaLine.branch)
        assertEquals(null, coberturaLine.conditionCoverage)
        assertEquals(null, coberturaLine.conditions)
    }

    @Test
    fun `Line constructor sets hits based on ci value`() {
        val jacocoLineWithHits = JacocoModels.Line().apply {
            nr = 10
            ci = 5  // covered instructions > 0
            mb = 0
            cb = 0
        }
        
        val jacocoLineWithoutHits = JacocoModels.Line().apply {
            nr = 11
            ci = 0  // no covered instructions
            mb = 0
            cb = 0
        }
        
        val coberturaLineWithHits = CoberturaModels.Line(jacocoLineWithHits)
        val coberturaLineWithoutHits = CoberturaModels.Line(jacocoLineWithoutHits)
        
        assertEquals(1, coberturaLineWithHits.hits)
        assertEquals(0, coberturaLineWithoutHits.hits)
    }
}