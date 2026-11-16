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
    fun `duplicate method names across classes collapse with last one winning`() {
        val pack = packageWith(
            classes = listOf(
                javaClass("com/example/Foo", "Foo.java", listOf(method("init", 12))),
                javaClass("com/example/FooHelper", "Foo.java", listOf(method("init", 15))),
                javaClass("com/example/FooOwner", "Foo.java", listOf(method("run", 10)))
            ),
            sourcefiles = listOf(sourceFile("Foo.java", 10..20))
        )

        // init collapses to line 15 (the later class wins), so run@10 runs up to 14;
        // first-wins or no collapse would truncate at 12
        assertEquals((10..14).toList(), numbers(pack.classes[2], "run"))
    }

    @Test
    fun `two methods starting on the same line do not truncate each other`() {
        val pack = packageWith(
            classes = listOf(
                javaClass("com/example/Foo", "Foo.java", listOf(method("a", 10), method("b", 10), method("c", 20)))
            ),
            sourcefiles = listOf(sourceFile("Foo.java", 10..25))
        )

        assertEquals((10..19).toList(), numbers(pack.classes[0], "a"))
        assertEquals((10..19).toList(), numbers(pack.classes[0], "b"))
        assertEquals((20..25).toList(), numbers(pack.classes[0], "c"))
    }

    @Test
    fun `method from another class in the same sourcefile truncates the boundary`() {
        val pack = packageWith(
            classes = listOf(
                javaClass("com/example/Foo", "Foo.java", listOf(method("run", 10))),
                javaClass("com/example/Bar", "Foo.java", listOf(method("stop", 13)))
            ),
            sourcefiles = listOf(sourceFile("Foo.java", 10..20))
        )

        assertEquals(listOf(10, 11, 12), numbers(pack.classes[0], "run"))
    }

    @Test
    fun `method with null line yields empty lines`() {
        val pack = packageWith(
            classes = listOf(
                javaClass("com/example/Foo", "Foo.java", listOf(method("run", 10), method("gap", null)))
            ),
            sourcefiles = listOf(sourceFile("Foo.java", 10..15))
        )

        assertTrue(numbers(pack.classes[0], "gap").isEmpty())
        assertEquals((10..15).toList(), numbers(pack.classes[0], "run"))
    }

    @Test
    fun `class with neither sourcefilename nor name yields empty lines`() {
        val pack = packageWith(
            classes = listOf(javaClass(null, null, listOf(method("run", 10)))),
            sourcefiles = listOf(sourceFile("Foo.java", 10..15))
        )

        assertTrue(numbers(pack.classes[0], "run").isEmpty())
    }

    private fun packageWith(
        classes: List<JacocoModels.ClassElement>,
        sourcefiles: List<JacocoModels.SourceFile>
    ): CoberturaModels.Package =
        CoberturaModels.Package(
            JacocoModels.PackageElement().apply {
                name = "com/example"
                this.classes = classes
                this.sourcefiles = sourcefiles
            }
        )

    private fun javaClass(
        name: String?,
        sourcefilename: String?,
        methods: List<JacocoModels.MethodElement>
    ): JacocoModels.ClassElement = JacocoModels.ClassElement().apply {
        this.name = name
        this.sourcefilename = sourcefilename
        this.methods = methods
    }

    private fun method(name: String, line: Int?): JacocoModels.MethodElement =
        JacocoModels.MethodElement().apply {
            this.name = name
            this.line = line
        }

    private fun sourceFile(name: String, nrs: IntRange): JacocoModels.SourceFile =
        JacocoModels.SourceFile().apply {
            this.name = name
            lines = nrs.map { nr -> JacocoModels.Line().apply { this.nr = nr } }
        }

    private fun numbers(owner: CoberturaModels.ClassElement, methodName: String): List<Int> =
        owner.methods.filter { it.name == methodName }.flatMap { it.lines.map { line -> line.number } }

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