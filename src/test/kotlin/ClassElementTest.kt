import net.razvan.Cobertura
import net.razvan.Jacoco
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ClassElementTest {
    @Test
    fun `initializes with correct values`() {
        val jacocoClassElement = Jacoco.ClassElement().apply {
            name = "TestClass"
            sourcefilename = "TestClass.kt"
            counters = listOf(
                Jacoco.Counter().apply {
                    type = "LINE"
                    missed = 5
                    covered = 10
                },
                Jacoco.Counter().apply {
                    type = "COMPLEXITY"
                    missed = 5
                    covered = 7
                },
                Jacoco.Counter().apply {
                    type = "BRANCH"
                    missed = 13
                    covered = 11
                }
            )
        }
        val jacocoPackageElement = Jacoco.PackageElement().apply {
            name = "TestPackage"
        }

        val classElement = Cobertura.ClassElement(jacocoClassElement, jacocoPackageElement)

        assertEquals("TestClass", classElement.name)
        assertEquals("TestPackage/TestClass.kt", classElement.filename)
        assertEquals(0.6666666666666666, classElement.lineRate)
        assertEquals(0.4583333333333333, classElement.branchRate)
        assertEquals(12.0, classElement.complexity)
    }

    @Test
    fun `handles null values gracefully`() {
        val jacocoClassElement = Jacoco.ClassElement()
        val jacocoPackageElement = Jacoco.PackageElement()

        val classElement = Cobertura.ClassElement(jacocoClassElement, jacocoPackageElement)

        assertEquals("", classElement.name)
        assertEquals("", classElement.filename)
        assertEquals(0.0, classElement.lineRate)
        assertEquals(0.0, classElement.branchRate)
        assertEquals(0.0, classElement.complexity)
    }

    @Test
    fun `removes root package prefix from filename`() {
        val jacocoClassElement = Jacoco.ClassElement().apply {
            name = "TestClass"
            sourcefilename = "TestClass.kt"
        }
        val jacocoPackageElement = Jacoco.PackageElement().apply {
            name = "com.example"
        }

        val classElement = Cobertura.ClassElement(jacocoClassElement, jacocoPackageElement, "com.example")

        assertEquals("TestClass.kt", classElement.filename)
    }


    @Test
    fun `replaces periods with slashes in filename path`() {
        val jacocoClassElement = Jacoco.ClassElement().apply {
            name = "TestClass"
            sourcefilename = "TestClass.kt"
        }
        val jacocoPackageElement = Jacoco.PackageElement().apply {
            name = "com.example"
        }

        val classElement = Cobertura.ClassElement(jacocoClassElement, jacocoPackageElement, null)

        assertEquals("com/example/TestClass.kt", classElement.filename)
    }
}
