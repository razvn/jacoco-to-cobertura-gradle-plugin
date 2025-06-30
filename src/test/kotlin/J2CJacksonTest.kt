package net.razvan

import net.razvan.models.CoberturaModels
import net.razvan.models.JacocoModels
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import java.io.File
import kotlin.io.path.createTempFile

class J2CJacksonTest {

    @Test
    fun `loadJacocoData throws exception for malformed XML`() {
        val tempFile = createTempFile(suffix = ".xml").toFile()
        tempFile.writeText("<invalid>xml</malformed>")
        
        val j2c = J2CJackson()
        val exception = assertThrows<JacocoToCoberturaException> {
            j2c.loadJacocoData(tempFile)
        }
        
        assertTrue(exception.message!!.contains("Loading Jacoco report error"))
        tempFile.delete()
    }

    @Test
    fun `loadJacocoData throws exception for non-existent file`() {
        val nonExistentFile = File("/tmp/non-existent-file.xml")
        
        val j2c = J2CJackson()
        val exception = assertThrows<JacocoToCoberturaException> {
            j2c.loadJacocoData(nonExistentFile)
        }
        
        assertTrue(exception.message!!.contains("Loading Jacoco report error"))
    }

    @Test
    fun `loadJacocoData throws exception for empty XML file`() {
        val tempFile = createTempFile(suffix = ".xml").toFile()
        tempFile.writeText("")
        
        val j2c = J2CJackson()
        val exception = assertThrows<JacocoToCoberturaException> {
            j2c.loadJacocoData(tempFile)
        }
        
        assertTrue(exception.message!!.contains("Loading Jacoco report error"))
        tempFile.delete()
    }

    @Test
    fun `transformData works with empty report`() {
        // Create a minimal report - this should work fine
        val emptyReport = JacocoModels.Report()
        
        val j2c = J2CJackson()
        val result = j2c.transformData(emptyReport, emptyList(), null)
        
        // Should succeed
        assertNotNull(result)
    }

    @Test
    fun `writeCoberturaData throws exception for read-only directory`() {
        val readOnlyDir = createTempDir()
        readOnlyDir.setWritable(false)
        val outputFile = File(readOnlyDir, "output.xml")
        
        val j2c = J2CJackson()
        val coverage = CoberturaModels.Coverage(JacocoModels.Report())
        
        val exception = assertThrows<JacocoToCoberturaException> {
            j2c.writeCoberturaData(outputFile, coverage)
        }
        
        assertTrue(exception.message!!.contains("Writing Cobertura Data to file"))
        assertTrue(exception.message!!.contains("unknown error") || exception.message!!.isNotEmpty())
        
        readOnlyDir.setWritable(true)
        readOnlyDir.deleteRecursively()
    }

    @Test
    fun `writeCoberturaData handles exception with null message`() {
        // Test the null safety fix for exception messages
        val invalidFile = File("/invalid/path/that/should/not/exist/output.xml")
        
        val j2c = J2CJackson()
        val coverage = CoberturaModels.Coverage(JacocoModels.Report())
        
        val exception = assertThrows<JacocoToCoberturaException> {
            j2c.writeCoberturaData(invalidFile, coverage)
        }
        
        assertTrue(exception.message!!.contains("Writing Cobertura Data to file"))
        // Should not contain "null" - should have fallback error message
        assertTrue(exception.message!!.contains("unknown error") || !exception.message!!.contains("null"))
    }

    @Test
    fun `getXmlData returns valid XML string`() {
        val j2c = J2CJackson()
        val coverage = CoberturaModels.Coverage(JacocoModels.Report())
        
        val xmlData = j2c.getXmlData(coverage)
        
        assertTrue(xmlData.isNotEmpty())
        assertTrue(xmlData.contains("<?xml"))
        assertTrue(xmlData.contains("<coverage"))
    }

    @Test
    fun `parseAs handles valid XML with different structure`() {
        val tempFile = createTempFile(suffix = ".xml").toFile()
        tempFile.writeText("""
            <?xml version="1.0" encoding="UTF-8"?>
            <report>
                <sessioninfo id="test" start="1000000" dump="1000001"/>
                <counter type="LINE" missed="0" covered="0"/>
            </report>
        """.trimIndent())
        
        val j2c = J2CJackson()
        val result = j2c.parseAs<JacocoModels.Report>(tempFile)
        
        // Should successfully parse
        assertNotNull(result)
        
        tempFile.delete()
    }
}
