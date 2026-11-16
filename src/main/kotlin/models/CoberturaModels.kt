package net.razvan.models

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText
import java.util.TreeSet

class CoberturaModels {

    abstract class Counters {
        @field:JacksonXmlProperty(localName = "line-rate", isAttribute = true)
        var lineRate: Double = 0.0

        @field:JacksonXmlProperty(localName = "branch-rate", isAttribute = true)
        var branchRate: Double = 0.0

        @field:JacksonXmlProperty(isAttribute = true)
        var complexity: Double? = null

        fun replaceSlashWithDot(s: String): String = s.replace("/", ".")
    }

    @JacksonXmlRootElement(localName = "coverage")
    class Coverage(
        j: JacocoModels.Report,
        sources: Collection<String> = emptyList(),
        rootPackageToRemove: String? = null
    ) :
        Counters() {
        @field:JacksonXmlProperty(isAttribute = true)
        val timestamp: Long = j.timestamp()

        @JacksonXmlElementWrapper(useWrapping = true, localName = "sources")
        @field:JacksonXmlProperty(localName = "source")
        val sources: List<Source> = (sources.takeIf { it.isNotEmpty() } ?: listOf(".")).map { Source(it) }

        @JacksonXmlElementWrapper(useWrapping = true, localName = "packages")
        @field:JacksonXmlProperty(localName = "package")
        val packages: List<Package> = j.packages.map { Package(it, rootPackageToRemove) }

        @field:JacksonXmlProperty(isAttribute = true)
        val version: String = "1.0"

        @field:JacksonXmlProperty(isAttribute = true, localName = "lines-covered")
        val linesCovered: Int

        @field:JacksonXmlProperty(isAttribute = true, localName = "lines-valid")
        val linesValid: Int

        @field:JacksonXmlProperty(isAttribute = true, localName = "branches-covered")
        val branchesCovered: Int

        @field:JacksonXmlProperty(isAttribute = true, localName = "branches-valid")
        val branchesValid: Int

        init {
            lineRate = j.lineRate()
            branchRate = j.branchRate()
            complexity = j.complexity()
            linesCovered = j.linesCovered()
            linesValid = j.linesValid()
            branchesCovered = j.branchesCovered()
            branchesValid = j.branchesValid()
        }
    }

    @JacksonXmlRootElement(localName = "package")
    class Package(p: JacocoModels.PackageElement, rootPackageToRemove: String? = null) : Counters() {
        @field:JacksonXmlProperty(isAttribute = true)
        val name: String = replaceSlashWithDot(p.name ?: "")

        @JacksonXmlElementWrapper(useWrapping = true, localName = "classes")
        @field:JacksonXmlProperty(localName = "class")
        val classes: List<ClassElement> = p.classes.map { ClassElement(it, p, rootPackageToRemove) }

        init {
            lineRate = p.lineRate()
            branchRate = p.branchRate()
            complexity = p.complexity()
        }
    }

    @JacksonXmlRootElement(localName = "class")
    class ClassElement(
        c: JacocoModels.ClassElement,
        jPack: JacocoModels.PackageElement,
        rootPackageToRemove: String? = null
    ) : Counters() {
        @field:JacksonXmlProperty(isAttribute = true)
        val name: String = replaceSlashWithDot(c.name ?: "")

        @field:JacksonXmlProperty(isAttribute = true)
        val filename: String = "${cleanPackageName(jPack.name, rootPackageToRemove)}${
            getSourceName(
                c.sourcefilename,
                c.name,
                jPack.sourcefiles
            )
        }"

        private val lineIndex = MethodLineIndex(jPack, c.sourcefilename, c.name?.substringAfterLast("/"))

        @JacksonXmlElementWrapper(useWrapping = true, localName = "methods")
        @field:JacksonXmlProperty(localName = "method")
        val methods: List<Method> = c.methods.map { Method(it, lineIndex) }

        @JacksonXmlElementWrapper(useWrapping = true, localName = "lines")
        @field:JacksonXmlProperty(localName = "line")
        val lines: List<Line>


        init {
            lineRate = c.lineRate()
            branchRate = c.branchRate()
            complexity = c.complexity()
            lines = methods.flatMap { it.lines }
        }

        private fun getSourceName(
            sourceFileName: String?,
            name: String?,
            sourcesFiles: List<JacocoModels.SourceFile>
        ): String {
            val nameInfo = sourceFileName ?: name?.substringAfterLast("/")
            return nameInfo?.let { n ->
                sourcesFiles
                    .mapNotNull { it.name }
                    .firstOrNull { it.startsWith("$n.") }
                    ?: n
            } ?: ""
        }

        private fun cleanPackageName(name: String?, rootPackageToRemove: String?): String {
            val pkg = name?.replace(".", "/") ?: ""
            val pkgToRemove = rootPackageToRemove?.replace(".", "/") ?: ""

            return pkgToRemove.let { pkg.removePrefix(it) }.let {
                val path = it.removePrefix(".").removePrefix("/")
                if (path.isNotEmpty()) "$path/" else ""
            }
        }
    }

    @JacksonXmlRootElement(localName = "method")
    class Method internal constructor(m: JacocoModels.MethodElement, index: MethodLineIndex) :
        Counters() {
        @field:JacksonXmlProperty(isAttribute = true)
        val name: String = m.name ?: ""

        @field:JacksonXmlProperty(isAttribute = true)
        val signature: String = m.desc ?: ""

        @JacksonXmlElementWrapper(useWrapping = true, localName = "lines")
        @field:JacksonXmlProperty(localName = "line")
        val lines: List<Line> = index.linesFor(m.line).map { Line(it) }

        init {
            lineRate = m.lineRate()
            branchRate = m.branchRate()
            complexity = null
        }
    }

    @JacksonXmlRootElement(localName = "line")
    class Line(l: JacocoModels.Line) {
        @field:JacksonXmlProperty(isAttribute = true)
        var number: Int = l.nr

        @field:JacksonXmlProperty(isAttribute = true)
        var hits: Int = if (l.ci > 0) 1 else 0 // not sure

        @field:JacksonXmlProperty(isAttribute = true)
        var branch: Boolean = false

        @field:JacksonXmlProperty(localName = "condition-coverage", isAttribute = true)
        var conditionCoverage: String? = null


        @JacksonXmlElementWrapper(useWrapping = true, localName = "conditions")
        @field:JacksonXmlProperty(localName = "condition")
        var conditions: List<Condition>? = null

        init {
            val mbcb = l.mb + l.cb
            if (mbcb > 0) {
                branch = true

                val percentage = (100 * (l.cb.toDouble() / mbcb)).toInt().toString() + "%"

                conditionCoverage = "$percentage (${l.cb}/$mbcb)"
                conditions = listOf(Condition(percentage))
            }
        }
    }

    @JacksonXmlRootElement(localName = "condition")
    class Condition(s: String) {
        @field:JacksonXmlProperty(isAttribute = true)
        val number: Int = 0

        @field:JacksonXmlProperty(isAttribute = true)
        val type: String = "jump"

        @field:JacksonXmlProperty(isAttribute = true)
        var coverage: String = s
    }

    @JacksonXmlRootElement(localName = "source")
    class Source(s: String = "") {
        @field:JacksonXmlText
        val value: String = s
    }

}

// Built once per Cobertura ClassElement: its (jSource, jName) context is a per-class constant,
// only the queried method line varies. The predicates and the toMap collapse below reproduce
// the previous per-method scan exactly, quirk for quirk.
internal class MethodLineIndex(
    jPack: JacocoModels.PackageElement,
    jSource: String?,
    jName: String?,
) {
    private val lineRuns: List<List<JacocoModels.Line>> =
        jPack.sourcefiles
            .filter { it.name?.let { name -> name == jSource || name.substringBeforeLast(".") == jName } == true }
            .map { it.lines.sortedBy { line -> line.nr } }

    private val starts: TreeSet<Int> =
        jPack.classes
            .filter { aClass ->
                when {
                    aClass.sourcefilename != null -> aClass.sourcefilename == jSource || aClass.sourcefilename?.substringBeforeLast(
                        "."
                    ) == jName

                    aClass.name != null -> aClass.name == jSource || aClass.name?.substringAfterLast("/") == jName
                    else -> false
                }
            }
            .flatMap { it.methods }
            .mapNotNull { method ->
                val name = method.name
                val line = method.line
                if (name != null && line != null) name to line else null
            }
            .toMap() // later duplicate method names overwrite — pinned semantics, not a bug
            .values.toCollection(TreeSet())

    private val noContext = jSource == null && jName == null

    fun linesFor(methodLine: Int?): List<JacocoModels.Line> {
        if (methodLine == null || noContext) return emptyList()
        val next = starts.higher(methodLine) ?: Int.MAX_VALUE
        return lineRuns.flatMap { run ->
            val from = run.lowerBound(methodLine)
            val to = run.lowerBound(next)
            if (from < to) run.subList(from, to) else emptyList()
        }
    }

    private fun List<JacocoModels.Line>.lowerBound(nr: Int): Int {
        var lo = 0
        var hi = size
        while (lo < hi) {
            val mid = (lo + hi) / 2
            if (this[mid].nr < nr) lo = mid + 1 else hi = mid
        }
        return lo
    }
}



