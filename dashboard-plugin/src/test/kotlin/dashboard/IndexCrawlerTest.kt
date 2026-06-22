package dashboard

import dashboard.model.BoroughStatus
import dashboard.model.EpicStatus
import dashboard.model.SessionActivity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class IndexCrawlerTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var crawler: IndexCrawler

    @BeforeEach
    fun setUp() {
        crawler = IndexCrawler()
    }

    @Test
    fun `crawlIndex should parse borough and epic tables`() {
        val indexAdoc = """
            = Index — BAKERY
            |===
            | Borough | Project | DAG | Role in MVP0 | Session
            | Newark | training-gradle | N2 | Pipeline formation | 010 ✅
            | BAKERY | bakery-gradle | N2 | Site statique | S001
            |===
            |===
            | EPIC | Subject | Pts | Priority | Status
            | DSH-0 | Bootstrap governance | 3 | P0 | ✅ S000
            | DSH-1 | Plugin scaffold | 8 | P0 | 🔄 S001
            | DSH-2 | Crawler | 13 | P0 | PLANIFIE
            |===
        """.trimIndent()
        val file = tempDir.resolve("INDEX.adoc")
        Files.writeString(file, indexAdoc)

        val data = crawler.crawlIndex(file)

        assertThat(data.boroughs).hasSize(2)
        assertThat(data.boroughs[0].name).isEqualTo("Newark")
        assertThat(data.boroughs[0].project).isEqualTo("training-gradle")
        assertThat(data.boroughs[0].dagLevel).isEqualTo("N2")
        assertThat(data.boroughs[1].name).isEqualTo("BAKERY")

        assertThat(data.epics).hasSize(3)
        assertThat(data.epics[0].id).isEqualTo("DSH-0")
        assertThat(data.epics[0].status).isEqualTo(EpicStatus.TERMINE)
        assertThat(data.epics[1].id).isEqualTo("DSH-1")
        assertThat(data.epics[1].status).isEqualTo(EpicStatus.EN_COURS)
        assertThat(data.epics[2].id).isEqualTo("DSH-2")
        assertThat(data.epics[2].status).isEqualTo(EpicStatus.PLANIFIE)
    }

    @Test
    fun `crawlIndex should return empty data for file without tables`() {
        val file = tempDir.resolve("EMPTY.adoc")
        Files.writeString(file, "= Just a title\n\nNo tables here.\n")

        val data = crawler.crawlIndex(file)

        assertThat(data.boroughs).isEmpty()
        assertThat(data.epics).isEmpty()
    }

    @Test
    fun `crawlIndex should infer VESTIGE borough status from role`() {
        val indexAdoc = """
            |===
            | Borough | Project | DAG | Role in MVP0 | Session
            | Nashville | newpipe-gradle | N2 | VESTIGE | 001 ✅
            |===
        """.trimIndent()
        val file = tempDir.resolve("INDEX.adoc")
        Files.writeString(file, indexAdoc)

        val data = crawler.crawlIndex(file)

        assertThat(data.boroughs).hasSize(1)
        assertThat(data.boroughs[0].name).isEqualTo("Nashville")
        assertThat(data.boroughs[0].status).isEqualTo(BoroughStatus.VESTIGE)
    }

    @Test
    fun `crawlDirectory should collect all INDEX_adoc files`() {
        val bakeryDir = tempDir.resolve("bakery")
        Files.createDirectories(bakeryDir)
        Files.writeString(bakeryDir.resolve("INDEX.adoc"), """
            |===
            | Borough | Project | DAG | Role in MVP0 | Session
            | BAKERY | bakery-gradle | N2 | Site statique | S001
            |===
        """.trimIndent())

        val newarkDir = tempDir.resolve("newark")
        Files.createDirectories(newarkDir)
        Files.writeString(newarkDir.resolve("INDEX.adoc"), """
            |===
            | Borough | Project | DAG | Role in MVP0 | Session
            | Newark | training-gradle | N2 | Pipeline formation | 010 ✅
            |===
        """.trimIndent())

        val data = crawler.crawlDirectory(tempDir)

        assertThat(data.boroughs).hasSize(2)
        assertThat(data.boroughs.map { it.name }).containsExactlyInAnyOrder("BAKERY", "Newark")
    }

    @Test
    fun `crawlDirectory should skip non-INDEX_adoc files`() {
        Files.writeString(tempDir.resolve("README.adoc"), "= Readme")
        val subDir = tempDir.resolve("sub")
        Files.createDirectories(subDir)
        Files.writeString(subDir.resolve("INDEX.adoc"), """
            |===
            | Borough | Project | DAG | Role in MVP0 | Session
            | Test | test-plugin | N0 | Testing | S000
            |===
        """.trimIndent())

        val data = crawler.crawlDirectory(tempDir)

        assertThat(data.boroughs).hasSize(1)
        assertThat(data.boroughs[0].name).isEqualTo("Test")
    }

    @Test
    fun `crawlSessionsHistory should parse session activities`() {
        val sessionsAdoc = """
            = SESSIONS_HISTORY
            |===
            | # | Date | Subject | Files
            | 000 | 2026-06-18 | Bootstrap gouvernance | 8 fichiers
            | 001 | 2026-06-18 | Plugin scaffold | 7 fichiers
            |===
        """.trimIndent()
        val file = tempDir.resolve("SESSIONS_HISTORY.adoc")
        Files.writeString(file, sessionsAdoc)

        val sessions = crawler.crawlSessionsHistory(file)

        assertThat(sessions).hasSize(2)
        assertThat(sessions[0]).isEqualTo(SessionActivity("000", "2026-06-18", "", "Bootstrap gouvernance", "", "8 fichiers"))
        assertThat(sessions[1]).isEqualTo(SessionActivity("001", "2026-06-18", "", "Plugin scaffold", "", "7 fichiers"))
    }

    @Test
    fun `crawlDirectory should attach sessions to their borough`() {
        val dashboardDir = tempDir.resolve("dashboard-gradle/dashboard-plugin")
        Files.createDirectories(dashboardDir)
        Files.writeString(dashboardDir.resolve("INDEX.adoc"), """
            |===
            | Borough | Project | DAG | Role in MVP0 | Session
            | Dashboard | dashboard-gradle | N3 | Dashboard vision | S003
            |===
        """.trimIndent())
        Files.writeString(dashboardDir.resolve("SESSIONS_HISTORY.adoc"), """
            |===
            | # | Date | Subject | Files
            | 003 | 2026-06-19 | Renommage ALGER | 15 fichiers
            |===
        """.trimIndent())

        val data = crawler.crawlDirectory(tempDir)

        assertThat(data.boroughs).hasSize(1)
        assertThat(data.sessions).hasSize(1)
        assertThat(data.sessions[0].borough).isEqualTo("Dashboard")
        assertThat(data.sessions[0].subject).isEqualTo("Renommage ALGER")
    }
}
