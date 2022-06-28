import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class JacocoToCoberturaPluginTest {

    @Test
    fun `task exists`() {
        val project = ProjectBuilder.builder().build()

        project.pluginManager.apply("net.razvan.jacoco-to-cobertura")
        assertDoesNotThrow {
            project.tasks.getByName("jacocoToCobertura")
        }
    }
}
