package net.razvan

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator
import net.razvan.models.CoberturaModels
import net.razvan.models.JacocoModels
import java.io.File

class J2CJackson {
    val xmlMapper = XmlMapper(
        JacksonXmlModule().apply { setDefaultUseWrapper(false) }
    ).apply {
        enable(SerializationFeature.INDENT_OUTPUT)
        enable(SerializationFeature.WRAP_ROOT_VALUE)
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        enable(ToXmlGenerator.Feature.WRITE_XML_DECLARATION)
    }

    inline fun <reified T : Any> parseAs(file: File): T {
        return xmlMapper.readValue(file, T::class.java)
    }

    fun loadJacocoData(fileIn: File): JacocoModels.Report = try {
        parseAs<JacocoModels.Report>(fileIn)
    } catch (e: Exception) {
        throw JacocoToCoberturaException("Loading Jacoco report error: `${e.message}`")
    }

    fun transformData(jacocoData: JacocoModels.Report, sources: Collection<String>, rootPackageToRemove: String?) = try {
        CoberturaModels.Coverage(jacocoData, sources, rootPackageToRemove)
    } catch (e: Exception) {
        throw JacocoToCoberturaException("Transforming Jacoco Data to Cobertura error: `${e.message}`")
    }

    fun writeCoberturaData(outputFile: File, data: CoberturaModels.Coverage) = with(outputFile) {
        try {
            writeText(getXmlData(data))
        } catch (e: Exception) {
            throw JacocoToCoberturaException("Writing Cobertura Data to file `${this.canonicalPath}` error: `${e.message}`")
        }
    }

    fun getXmlData(data: CoberturaModels.Coverage): String {
        return xmlMapper.writeValueAsString(data)
    }
}
