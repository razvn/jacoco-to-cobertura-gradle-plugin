package net.razvan.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

class JacocoModels {
    interface Counters {
        var counters: List<Counter>

        fun branchRate(): Double = counter("BRANCH", counters)
        fun lineRate(): Double = counter("LINE", counters)
        fun complexity(): Double = counter("COMPLEXITY", counters, Companion::sum)
        fun branchesCovered(): Int = counterCoveredValue("BRANCH", counters)
        fun branchesValid(): Int = counterValidValue("BRANCH", counters)
        fun linesCovered(): Int = counterCoveredValue("LINE", counters)
        fun linesValid(): Int = counterValidValue("LINE", counters)

        companion object {
            fun counter(type: String, counters: List<Counter>, op: (Int, Int) -> Double = Companion::fraction) =
                counters.firstOrNull { it.type == type }?.let {
                    op(it.covered, it.missed)
                } ?: 0.0

            fun sum(s1: Int, s2: Int): Double = s1.toDouble() + s2
            fun fraction(s1: Int, s2: Int): Double = if (s1 != 0 || s2 != 0) (s1.toDouble() / (s1 + s2)) else 0.0

            fun counterCoveredValue(type: String, counter: List<Counter>) =
                counter.firstOrNull { it.type == type }?.covered?.toInt() ?: 0

            fun counterValidValue(type: String, counter: List<Counter>): Int {
                val count = counter.firstOrNull { it.type == type }
                val missed = count?.missed?.toInt() ?: 0
                val covered = count?.covered?.toInt() ?: 0
                return missed + covered
            }

        }
    }

    @JacksonXmlRootElement(localName = "report")
    data class Report(
        @set:JsonProperty("package", required = false)
        var packages: List<PackageElement> = mutableListOf()
    ) : Counters {
        @field:JacksonXmlProperty(isAttribute = true)
        lateinit var name: String

        @field:JacksonXmlProperty(localName = "sessioninfo")
        var sessionInfos: List<SessionInfo> = mutableListOf()

        @field:JacksonXmlProperty(localName = "counter")
        override var counters: List<Counter> = mutableListOf()

        fun timestamp(): Long = sessionInfos.firstOrNull()?.start?.toLongOrNull()?.let { it / 1000 } ?: 0

        fun packagesNames() = packages.mapNotNull(PackageElement::name).toSet()

        fun sources() = packages.flatMap { p ->
            p.sourcefiles.mapNotNull { s ->
                s.name?.let { (p.name ?: "") + "/" + it }
            }
        }
    }

    @JacksonXmlRootElement(localName = "sessioninfo")
    class SessionInfo {
        @field:JacksonXmlProperty(isAttribute = true)
        var id: String? = null

        @field:JacksonXmlProperty(isAttribute = true)
        var start: String? = null

        @field:JacksonXmlProperty(isAttribute = true)
        var dump: String? = null
    }

    @JacksonXmlRootElement(localName = "package")
    class PackageElement : Counters {
        @field:JacksonXmlProperty( isAttribute = true)
        var name: String? = null

        @field:JacksonXmlProperty(localName = "class")
        var classes: List<ClassElement> = mutableListOf()

        @field:JacksonXmlProperty(localName = "sourcefile")
        var sourcefiles: List<SourceFile> = mutableListOf()

        @field:JacksonXmlProperty(localName = "counter")
        override var counters: List<Counter> = mutableListOf()
    }

    @JacksonXmlRootElement(localName = "sourcefile")
    class SourceFile : Counters {
        @field:JacksonXmlProperty(isAttribute = true)
        var name: String? = null

        @field:JacksonXmlProperty(localName = "line")
        var lines: List<Line> = mutableListOf()

        @field:JacksonXmlProperty(localName = "counter")
        override var counters: List<Counter> = mutableListOf()
    }

    @JacksonXmlRootElement(localName = "line")
    class Line {
        @field:JacksonXmlProperty(isAttribute = true)
        var nr: Int = 0

        @field:JacksonXmlProperty(isAttribute = true)
        var mi: Int = 0

        @field:JacksonXmlProperty(isAttribute = true)
        var ci: Int = 0

        @field:JacksonXmlProperty(isAttribute = true)
        var mb: Int = 0

        @field:JacksonXmlProperty(isAttribute = true)
        var cb: Int = 0
    }

    @JacksonXmlRootElement(localName = "class")
    class ClassElement : Counters {
        @field:JacksonXmlProperty(isAttribute = true)
        var name: String? = null

        @field:JacksonXmlProperty(isAttribute = true)
        var sourcefilename: String? = null

        @field:JacksonXmlProperty(localName = "method")
        var methods: List<MethodElement> = mutableListOf()

        @field:JacksonXmlProperty(localName = "counter")
        override var counters: List<Counter> = mutableListOf()
    }

    @JacksonXmlRootElement(localName = "method")
    class MethodElement : Counters {
        @field:JacksonXmlProperty(isAttribute = true)
        var name: String? = null

        @field:JacksonXmlProperty(isAttribute = true)
        var desc: String? = null

        @field:JacksonXmlProperty(isAttribute = true)
        var line: Int? = null

        @field:JacksonXmlProperty(localName = "counter")
        override var counters: List<Counter> = mutableListOf()
    }

    @JacksonXmlRootElement(localName = "counter")
    class Counter {
        @field:JacksonXmlProperty(isAttribute = true)
        var type: String? = null

        @field:JacksonXmlProperty(isAttribute = true)
        var missed: Int = 0

        @field:JacksonXmlProperty(isAttribute = true)
        var covered: Int = 0
    }
}
