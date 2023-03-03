import net.razvan.JacocoToCoberturaPlugin
import org.gradle.kotlin.dsl.apply
import org.gradle.testfixtures.ProjectBuilder
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
}
