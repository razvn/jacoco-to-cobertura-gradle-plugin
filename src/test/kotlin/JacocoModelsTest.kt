package net.razvan.models

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JacocoModelsTest {

    @Test
    fun `branchRate returns zero when no BRANCH counter exists`() {
        val element = JacocoModels.PackageElement().apply {
            counters = listOf(
                JacocoModels.Counter().apply { 
                    type = "LINE"
                    covered = 5
                    missed = 5 
                }
            )
        }
        
        assertEquals(0.0, element.branchRate())
    }

    @Test
    fun `lineRate returns zero when no LINE counter exists`() {
        val element = JacocoModels.ClassElement().apply {
            counters = listOf(
                JacocoModels.Counter().apply { 
                    type = "BRANCH"
                    covered = 3
                    missed = 7 
                }
            )
        }
        
        assertEquals(0.0, element.lineRate())
    }

    @Test
    fun `complexity returns zero when no COMPLEXITY counter exists`() {
        val element = JacocoModels.MethodElement().apply {
            counters = listOf(
                JacocoModels.Counter().apply { 
                    type = "LINE"
                    covered = 1
                    missed = 0 
                }
            )
        }
        
        assertEquals(0.0, element.complexity())
    }

    @Test
    fun `counter method handles null type gracefully`() {
        val counters = listOf(
            JacocoModels.Counter().apply { 
                type = null
                covered = 5
                missed = 5 
            }
        )
        
        assertEquals(0.0, JacocoModels.Counters.counter("BRANCH", counters))
    }

    @Test
    fun `counter method uses orEmpty for null type comparison`() {
        val counters = listOf(
            JacocoModels.Counter().apply { 
                type = null
                covered = 10
                missed = 0 
            },
            JacocoModels.Counter().apply { 
                type = ""
                covered = 5
                missed = 5 
            }
        )
        
        // Should find the first counter that matches empty string (null becomes empty with orEmpty)
        // Since null counter is first and orEmpty() converts it to "", it will find the null counter first
        assertEquals(1.0, JacocoModels.Counters.counter("", counters))
    }

    @Test
    fun `getCounterMap handles null types with orEmpty`() {
        val element = JacocoModels.Report().apply {
            counters = listOf(
                JacocoModels.Counter().apply { 
                    type = null
                    covered = 1
                    missed = 1 
                },
                JacocoModels.Counter().apply { 
                    type = "BRANCH"
                    covered = 2
                    missed = 2 
                }
            )
        }
        
        val counterMap = element.getCounterMap()
        
        assertTrue(counterMap.containsKey(""))  // null becomes empty string
        assertTrue(counterMap.containsKey("BRANCH"))
        assertEquals(2, counterMap.size)
    }

    @Test
    fun `counterCoveredValue returns zero for missing counter type`() {
        val counters = listOf(
            JacocoModels.Counter().apply { 
                type = "LINE"
                covered = 10
                missed = 5 
            }
        )
        
        assertEquals(0, JacocoModels.Counters.counterCoveredValue("BRANCH", counters))
    }

    @Test
    fun `counterValidValue returns zero for missing counter type`() {
        val counters = listOf(
            JacocoModels.Counter().apply { 
                type = "LINE"
                covered = 10
                missed = 5 
            }
        )
        
        assertEquals(0, JacocoModels.Counters.counterValidValue("BRANCH", counters))
    }

    @Test
    fun `fraction calculation handles zero denominators`() {
        val result = JacocoModels.Counters.fraction(0, 0)
        assertEquals(0.0, result)
    }

    @Test
    fun `fraction calculation works with normal values`() {
        val result = JacocoModels.Counters.fraction(3, 7)
        assertEquals(0.3, result, 0.001)
    }

    @Test
    fun `sum calculation converts to double`() {
        val result = JacocoModels.Counters.sum(5, 3)
        assertEquals(8.0, result)
    }

    @Test
    fun `Report timestamp handles empty sessionInfos`() {
        val report = JacocoModels.Report().apply {
            sessionInfos = emptyList()
        }
        
        assertEquals(0L, report.timestamp())
    }

    @Test
    fun `Report timestamp handles null start values`() {
        val report = JacocoModels.Report().apply {
            sessionInfos = listOf(
                JacocoModels.SessionInfo().apply { start = null }
            )
        }
        
        assertEquals(0L, report.timestamp())
    }

    @Test
    fun `Report timestamp handles invalid start values`() {
        val report = JacocoModels.Report().apply {
            sessionInfos = listOf(
                JacocoModels.SessionInfo().apply { start = "not-a-number" }
            )
        }
        
        assertEquals(0L, report.timestamp())
    }

    @Test
    fun `Report timestamp converts milliseconds to seconds`() {
        val report = JacocoModels.Report().apply {
            sessionInfos = listOf(
                JacocoModels.SessionInfo().apply { start = "1000000" }  // 1 million ms
            )
        }
        
        assertEquals(1000L, report.timestamp())  // Should be divided by 1000
    }

    @Test
    fun `Report sources handles null package names`() {
        val report = JacocoModels.Report().apply {
            packagesReport = listOf(
                JacocoModels.PackageElement().apply { 
                    name = null
                    sourcefiles = listOf(
                        JacocoModels.SourceFile().apply { name = "Test.java" }
                    )
                }
            )
        }
        
        val sources = report.sources()
        assertEquals(1, sources.size)
        assertEquals("/Test.java", sources[0])  // null package becomes empty, then "/"
    }

    @Test
    fun `Report sources handles null sourcefile names`() {
        val report = JacocoModels.Report().apply {
            packagesReport = listOf(
                JacocoModels.PackageElement().apply { 
                    name = "com/example"
                    sourcefiles = listOf(
                        JacocoModels.SourceFile().apply { name = null }
                    )
                }
            )
        }
        
        val sources = report.sources()
        assertTrue(sources.isEmpty())  // null sourcefile names are filtered out
    }

    @Test
    fun `Report packages property prefers packagesReport over groups`() {
        val directPackage = JacocoModels.PackageElement().apply { name = "direct" }
        val report = JacocoModels.Report().apply {
            packagesReport = listOf(directPackage)
            groups = listOf(
                JacocoModels.Group().apply {
                    packages = listOf(
                        JacocoModels.PackageElement().apply { name = "grouped" }
                    )
                }
            )
        }
        
        val packages = report.packages
        assertEquals(1, packages.size)
        assertEquals("direct", packages[0].name)
    }
}