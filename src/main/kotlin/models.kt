package net.razvan

import org.simpleframework.xml.Attribute
import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Path
import org.simpleframework.xml.Root
import org.simpleframework.xml.Text

class Cobertura {
    abstract class Counters {
        @field:Attribute(name = "line-rate", required = false)
        var lineRate: Double = 0.0

        @field:Attribute(name = "branch-rate", required = false)
        var branchRate: Double = 0.0

        @field:Attribute(name = "complexity", required = false)
        var complexity: Double = 0.0
    }

    @Root(strict = false, name = "coverage")
    class Coverage(j: Jacoco.Report, sources: Collection<String> = emptyList()) : Counters() {
        @field:Attribute(name = "timestamp", required = true)
        val timestamp: Long = j.timestamp()

        @field:Path("sources") //workaround for not adding an attribut class="ArrayList" in <sources>
        @field:ElementList(name = "sources", required = false, inline = true)
        val sources: List<Source> = (sources.takeIf { it.isNotEmpty() } ?: listOf(".")).map { Source(it) }

        @field:Path("packages")
        @field:ElementList(name = "packages", required = false, inline = true)
        val packages: List<Package> = j.packages.map { Package(it) }

        init {
            lineRate = j.lineRate()
            branchRate = j.branchRate()
            complexity = j.complexity()
        }
    }

    @Root(strict = false, name = "package")
    class Package(p: Jacoco.PackageElement) : Counters() {
        @field:Attribute(name = "name", required = true)
        val name: String = p.name ?: ""

        @field:Path("classes")
        @field:ElementList(name = "classes", required = false, inline = true)
        val classes: List<ClassElement> = p.classes.map { ClassElement(it, p) }

        init {
            lineRate = p.lineRate()
            branchRate = p.branchRate()
            complexity = p.complexity()
        }
    }

    @Root(strict = false, name = "class")
    class ClassElement(c: Jacoco.ClassElement, jPack: Jacoco.PackageElement) : Counters() {
        @field:Attribute(name = "name", required = true)
        val name: String = c.name ?: ""

        @field:Attribute(name = "filename", required = true)
        val filename: String = "${jPack.name ?: ""}/${c.sourcefilename ?: ""}"

        @field:Path("methods")
        @field:ElementList(name = "methods", required = false, inline = true)
        val methods: List<Method> = c.methods.map { Method(m = it, jSource = c.sourcefilename, jPack = jPack) }

        init {
            lineRate = c.lineRate()
            branchRate = c.branchRate()
            complexity = c.complexity()
        }
    }

    @Root(strict = false, name = "method")
    class Method(m: Jacoco.MethodElement, jSource: String?, jPack: Jacoco.PackageElement) : Counters() {
        @field:Attribute(name = "name", required = true)
        val name: String = m.name ?: ""

        @field:Attribute(name = "signature", required = true)
        val signature: String = m.desc ?: ""

        @field:Path("lines")
        @field:ElementList(name = "lines", required = false, inline = true)
        val lines: List<Line> = linesForMethod(jMethod = m, jPack = jPack, jSource = jSource).map { Line(it) }

        init {
            lineRate = m.lineRate()
            branchRate = m.branchRate()
            complexity = m.complexity()
        }
    }

    @Root(strict = false, name = "line")
    class Line(l: Jacoco.Line) {
        @field:Attribute(name = "number", required = false)
        var number: Int = l.nr

        @field:Attribute(name = "hits", required = false)
        var hits: Int = if (l.ci > 0) 1 else 0 // not sure

        @field:Attribute(name = "branch", required = false)
        var branch: Boolean = false

        @field:Attribute(name = "condition-coverage", required = false)
        var conditionCoverage: String? = null

        // @field:Path("conditions")
        @field:ElementList(name = "conditions", required = false)
        var conditions: List<Condition>? = null

        init {
            if (l.mb + l.cb > 0) {
                branch = true

                val percentage = (100 * (l.cb.toDouble() / (l.cb + l.mb))).toInt().toString() + "%"

                conditionCoverage = "$percentage (${l.cb}/${l.cb + l.mb})"
                conditions = listOf(Condition(percentage))
            }
        }
    }

    @Root(strict = false, name = "condition")
    class Condition(s: String) {
        @field:Attribute(name = "number", required = false)
        val number: Int = 0

        @field:Attribute(name = "type", required = false)
        val type: String = "jump"

        @field:Attribute(name = "coverage", required = false)
        var coverage: String = s
    }

    @Root(strict = false, name = "source")
    class Source(s: String = "") {
        @field:Text
        val value: String = s
    }

    companion object {
        fun linesForMethod(
                jMethod: Jacoco.MethodElement,
                jPack: Jacoco.PackageElement,
                jSource: String?
        ): List<Jacoco.Line> {
            return if (jSource == null) emptyList()
            else {
                val currentMethodLine = jMethod.line ?: 0
                val sourceLines: List<Jacoco.Line> = jPack.sourcefiles
                        .filter { it.name == jSource }
                        .flatMap { it.lines }
                        .filter { it.nr >= currentMethodLine }

                val packMethods = jPack.classes
                        .filter { it.sourcefilename == jSource }
                        .flatMap { it.methods }
                        .filter { it.name != null && it.line != null }
                        .associateBy({ it.name!! }, { it.line!! })
                        .filterValues { it > currentMethodLine } // on garde que les methodes avec un numero de ligne supperieur

                val nextMethodLine = packMethods.minByOrNull { it.value }?.value ?: Int.MAX_VALUE
                return sourceLines.filter { it.nr < nextMethodLine }
            }
        }
    }
}

class Jacoco {
    interface Counters {
        var counters: List<Counter>

        fun branchRate(): Double = counter("BRANCH", counters)
        fun lineRate(): Double = counter("LINE", counters)
        fun complexity(): Double = counter("COMPLEXITY", counters, ::sum)

        companion object {
            fun counter(type: String, counters: List<Counter>, op: (Int, Int) -> Double = ::fraction) =
                    counters.firstOrNull { it.type == type }?.let {
                        op(it.covered, it.missed)
                    } ?: 0.0

            fun sum(s1: Int, s2: Int): Double = s1.toDouble() + s2
            fun fraction(s1: Int, s2: Int): Double = if (s1 != 0 || s2 != 0) (s1.toDouble() / (s1 + s2)) else 0.0
        }
    }

    @Root(strict = false, name = "report")
    data class Report(
        @field:ElementList(name = "package", required = false, inline = true)
        var packages: List<PackageElement> = mutableListOf()
    ) : Counters {
        @field:Attribute(name = "name", required = true)
        lateinit var name: String

        @field:ElementList(name = "sessioninfo", required = false, inline = true)
        var sessionInfos: List<SessionInfo> = mutableListOf()


        @field:ElementList(name = "counter", required = false, inline = true)
        override var counters: List<Counter> = mutableListOf()

        fun timestamp(): Long = sessionInfos.firstOrNull()?.start?.toLongOrNull()?.let { it / 1000 } ?: 0

        fun packagesNames() = packages.mapNotNull(PackageElement::name).toSet()

        fun sources() = packages.flatMap { p ->
            p.sourcefiles.mapNotNull { s ->
                s.name?.let { (p.name ?: "") + "/" + it }
            }
        }
    }

    @Root(name = "package", strict = false)
    class PackageElement : Counters {
        @field:Attribute(name = "name", required = false)
        var name: String? = null

        @field:ElementList(name = "class", required = false, inline = true)
        var classes: List<ClassElement> = mutableListOf()

        @field:ElementList(name = "sourcefile", required = false, inline = true)
        var sourcefiles: List<SourceFile> = mutableListOf()

        @field:ElementList(name = "counter", required = false, inline = true)
        override var counters: List<Counter> = mutableListOf()
    }

    @Root(name = "sourcefile", strict = false)
    class SourceFile : Counters {
        @field:Attribute(name = "name", required = false)
        var name: String? = null

        @field:ElementList(name = "line", required = false, inline = true)
        var lines: List<Line> = mutableListOf()

        @field:ElementList(name = "counter", required = false, inline = true)
        override var counters: List<Counter> = mutableListOf()
    }

    @Root(name = "line", strict = false)
    class Line {
        @field:Attribute(name = "nr", required = false)
        var nr: Int = 0

        @field:Attribute(name = "mi", required = false)
        var mi: Int = 0

        @field:Attribute(name = "ci", required = false)
        var ci: Int = 0

        @field:Attribute(name = "mb", required = false)
        var mb: Int = 0

        @field:Attribute(name = "cb", required = false)
        var cb: Int = 0
    }

    @Root(name = "class", strict = false)
    class ClassElement : Counters {
        @field:Attribute(name = "name", required = false)
        var name: String? = null

        @field:Attribute(name = "sourcefilename", required = false)
        var sourcefilename: String? = null

        @field:ElementList(name = "method", required = false, inline = true)
        var methods: List<MethodElement> = mutableListOf()

        @field:ElementList(name = "counter", required = false, inline = true)
        override var counters: List<Counter> = mutableListOf()
    }

    @Root(name = "method", strict = false)
    class MethodElement : Counters {
        @field:Attribute(name = "name", required = false)
        var name: String? = null

        @field:Attribute(name = "desc", required = false)
        var desc: String? = null

        @field:Attribute(name = "line", required = false)
        var line: Int? = null

        @field:ElementList(name = "counter", required = false, inline = true)
        override var counters: List<Counter> = mutableListOf()
    }

    @Root(name = "counter", strict = false)
    class Counter {
        @field:Attribute(name = "type", required = false)
        var type: String? = null

        @field:Attribute(name = "missed", required = false)
        var missed: Int = 0

        @field:Attribute(name = "covered", required = false)
        var covered: Int = 0
    }

    @Root(strict = false, name = "sessioninfo")
    class SessionInfo {
        @field:Attribute(name = "id", required = false)
        var id: String? = null

        @field:Attribute(name = "start", required = false)
        var start: String? = null

        @field:Attribute(name = "dump", required = false)
        var dump: String? = null
    }
}

fun Jacoco.Report.print() {
    println(name)
    println(sessionInfos.map(Jacoco.SessionInfo::id).joinToString(", "))
    packages.forEach {
        println("   package: ${it.name}")
        it.classes.forEach {
            println("       class: ${it.name} - ${it.sourcefilename}")
            it.methods.forEach {
                println("         ${it.name} - ${it.desc} - ${it.line}")
                it.counters.forEach {
                    println("               ${it.type} - ${it.missed} - ${it.covered}")
                }
            }

        }
        it.sourcefiles.forEach {
            println("       sourcefile: ${it.name}")
            it.lines.forEach {
                println("         line: ${it.mi} - ${it.ci} - ${it.cb}")
            }

            it.counters.forEach {
                println("         counter: ${it.type} - ${it.missed} - ${it.covered}")
            }
        }

        it.counters.forEach {
            println("   counter: ${it.type} - ${it.missed} - ${it.covered}")
        }
    }
    counters.forEach {
        println("counter: ${it.type} - ${it.missed} - ${it.covered}")
    }
}
