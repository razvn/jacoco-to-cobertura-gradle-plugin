package net.razvan.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import net.razvan.models.CounterTypes.BRANCH
import net.razvan.models.CounterTypes.COMPLEXITY
import net.razvan.models.CounterTypes.LINE

class JacocoModels {
    interface Counters {
        var counters: List<Counter>

        // Each implementing class should override this with lazy caching
        fun getCounterMap(): Map<String, Counter> = counters.associateBy { it.type.orEmpty() }

        fun branchRate(): Double = counter(BRANCH, counters)
        fun lineRate(): Double = counter(LINE, counters)
        fun complexity(): Double = counter(COMPLEXITY, counters, Companion::sum)
        fun branchesCovered(): Int = counterCoveredValue(BRANCH, counters)
        fun branchesValid(): Int = counterValidValue(BRANCH, counters)
        fun linesCovered(): Int = counterCoveredValue(LINE, counters)
        fun linesValid(): Int = counterValidValue(LINE, counters)

        companion object {
            fun counter(type: String, counters: List<Counter>, op: (Int, Int) -> Double = Companion::fraction) =
                counters.firstOrNull { it.type.orEmpty() == type }?.let {
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
        var packagesReport: List<PackageElement> = mutableListOf()
    ) : Counters {
        private val counter by lazy { counters.associateBy { it.type ?: "" } }
        override fun getCounterMap(): Map<String, Counter> = counter

        val packages: List<PackageElement>
            get() = if (packagesReport.isNotEmpty()) packagesReport else groups.flatMap { g ->
                val groupPrefix = (g.name?.let { "$it/" } ?: "")
                g.packages.map { pkg ->
                    PackageElement().apply {
                        name = groupPrefix + (pkg.name ?: "")
                        classes = pkg.classes
                        sourcefiles = pkg.sourcefiles
                        counters = pkg.counters
                    }
                }
            }

        @field:JacksonXmlProperty(isAttribute = true)
        var name: String? = null

        @field:JacksonXmlProperty(localName = "sessioninfo")
        var sessionInfos: List<SessionInfo> = mutableListOf()

        @field:JacksonXmlProperty(localName = "group")
        var groups: List<Group> = mutableListOf()

        @field:JacksonXmlProperty(localName = "counter")
        override var counters: List<Counter> = mutableListOf()

        fun timestamp(): Long = sessionInfos.firstOrNull()?.start?.toLongOrNull()?.let { it / 1000 } ?: 0

        fun packagesNames() = packages.mapNotNull(PackageElement::name).toSet()

        fun sources() = packages.asSequence()
            .flatMap { p ->
               p.sourcefiles.asSequence()
                   .mapNotNull { s -> s.name?.let { (p.name ?: "") + "/" + it } }
        }.toList()
    }

    @JacksonXmlRootElement(localName = "group")
    data class Group(
        @set:JsonProperty("package", required = false)
        var packages: List<PackageElement> = mutableListOf()
    ) : Counters {
        private val counter by lazy { counters.associateBy { it.type ?: "" } }
        override fun getCounterMap(): Map<String, Counter> = counter

        @field:JacksonXmlProperty(isAttribute = true)
        var name: String? = null

        @field:JacksonXmlProperty(localName = "counter")
        override var counters: List<Counter> = mutableListOf()
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
        private val counter by lazy { counters.associateBy { it.type ?: "" } }
        override fun getCounterMap(): Map<String, Counter> = counter

        @field:JacksonXmlProperty(isAttribute = true)
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
        private val counter by lazy { counters.associateBy { it.type ?: "" } }
        override fun getCounterMap(): Map<String, Counter> = counter

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
        private val counter by lazy { counters.associateBy { it.type ?: "" } }
        override fun getCounterMap(): Map<String, Counter> = counter

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
        private val counter by lazy { counters.associateBy { it.type ?: "" } }
        override fun getCounterMap(): Map<String, Counter> = counter

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
