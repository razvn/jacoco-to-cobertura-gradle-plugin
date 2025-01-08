package net.razvan

import org.simpleframework.xml.Serializer
import org.simpleframework.xml.core.Persister
import org.simpleframework.xml.stream.Format
import java.io.ByteArrayOutputStream
import java.io.File

object J2CSimpleXML {
    private val DOCTYPE_REGEX = "<!DOCTYPE.[^>]*>".toRegex()

    fun loadJacocoData(fileIn: File): JacocoSimpleXML.Report = try {
        val serializer: Serializer = Persister()
        val data = fileIn.readText().replace(DOCTYPE_REGEX, "")
        serializer.read(JacocoSimpleXML.Report::class.java, data)
    } catch (e: Exception) {
        throw JacocoToCoberturaException("Loading Jacoco report error: `${e.message}`")
    }

    fun transformData(jacocoData: JacocoSimpleXML.Report, sources: Collection<String>, rootPackageToRemove: String?) = try {
        CoberturaSimpleXML.Coverage(jacocoData, sources, rootPackageToRemove)
    } catch (e: Exception) {
        throw JacocoToCoberturaException("Transforming Jacoco Data to Cobertura error: `${e.message}`")
    }

    fun writeCoberturaData(outputFile: File, data: CoberturaSimpleXML.Coverage) = with(outputFile) {
        try {
            Persister(Format("<?xml version=\"1.0\" encoding= \"UTF-8\" ?>")).write(data, this)
        } catch (e: Exception) {
            throw JacocoToCoberturaException("Writing Cobertura Data to file `${this.canonicalPath}` error: `${e.message}`")
        }
    }

    fun getXmlData(data: CoberturaSimpleXML.Coverage): String {
        val out = ByteArrayOutputStream()
        return try {
            Persister(Format("<?xml version=\"1.0\" encoding= \"UTF-8\" ?>")).write(data, out)
            String(out.toByteArray())
        } catch (e: Exception) {
            throw JacocoToCoberturaException("Writing Cobertura Data string error: `${e.message}`")
        }
    }
}
