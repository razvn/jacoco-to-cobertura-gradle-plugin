package net.razvan

import org.http4k.core.ContentType
import org.http4k.testing.Approver
import org.http4k.testing.XmlApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File

@ExtendWith(XmlApprovalTest::class)
class JacocoJKTest {
    val j2c = J2CJackson()

    @Test
    @Disabled
    fun `compare jackson with sample xml`() {
        val file = File(this.javaClass.classLoader.getResource("jacoco-sample2.xml")!!.toURI().path)
        val sample = j2c.loadJacocoData(file)
        val transform = j2c.transformData(sample, emptyList(), null)
        val xmlData = j2c.getXmlData(transform)

        assertNotNull(sample)
        assertNotNull(transform)
        assertNotNull(xmlData)


        val samplSX = J2CSimpleXML.loadJacocoData(file)
        val transformSX = J2CSimpleXML.transformData(samplSX, emptyList(), null)
        val xmlDataSX = J2CSimpleXML.getXmlData(transformSX)
        j2c.writeCoberturaData(File("cobertura-jackson.xml"), transform)
        J2CSimpleXML.writeCoberturaData(File("cobertura-simple.xml"), transformSX)
        assertNotNull(xmlDataSX)
    }

    @Test
    // @Disabled
    fun `generate SampleXML file`() {
        val file = File(this.javaClass.classLoader.getResource("jacoco-sample2.xml")!!.toURI().path)
        val samplSX = J2CSimpleXML.loadJacocoData(file)
        val transformSX = J2CSimpleXML.transformData(samplSX, emptyList(), null)
        J2CSimpleXML.writeCoberturaData(File("cobertura-simple.xml"), transformSX)
    }

    @Test
    // @Disabled
    fun `generate Jackson file`() {
        val file = File(this.javaClass.classLoader.getResource("jacoco-sample2.xml")!!.toURI().path)
        val sample = j2c.loadJacocoData(file)
        val transform = j2c.transformData(sample, emptyList(), null)
        j2c.writeCoberturaData(File("cobertura-jackson.xml"), transform)
    }


    @Test
    fun `jackson for sample generation is valid`(approver: Approver) {
        val file = File(this.javaClass.classLoader.getResource("jacoco-sample.xml")!!.toURI().path)
        val sample = j2c.loadJacocoData(file)
        val transform = j2c.transformData(sample, emptyList(), null)
        approver.assertApproved(j2c.getXmlData(transform), ContentType.APPLICATION_XML)
    }

    @Test
    fun `jackson for sample2 generation is valid`(approver: Approver) {
        val file = File(this.javaClass.classLoader.getResource("jacoco-sample2.xml")!!.toURI().path)
        val sample = j2c.loadJacocoData(file)
        val transform = j2c.transformData(sample, emptyList(), null)
        approver.assertApproved(j2c.getXmlData(transform), ContentType.APPLICATION_XML)
    }

    @Test
    fun `jackson for sample3 generation is valid`(approver: Approver) {
        val file = File(this.javaClass.classLoader.getResource("jacoco-sample3.xml")!!.toURI().path)
        val sample = j2c.loadJacocoData(file)
        val transform = j2c.transformData(sample, listOf("src/main/kotlin"), "example")
        approver.assertApproved(j2c.getXmlData(transform), ContentType.APPLICATION_XML)
    }

    @Test
    fun `jackson for sample4 generation is valid`(approver: Approver) {
        val file = File(this.javaClass.classLoader.getResource("jacoco-sample4.xml")!!.toURI().path)
        val sample = j2c.loadJacocoData(file)
        val transform = j2c.transformData(sample, emptyList(), null)
        approver.assertApproved(j2c.getXmlData(transform), ContentType.APPLICATION_XML)
    }
}

