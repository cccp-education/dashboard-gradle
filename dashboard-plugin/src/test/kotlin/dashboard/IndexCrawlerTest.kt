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
        val indexAdoc =
            """
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
        val indexAdoc =
            """
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
        Files.writeString(
            bakeryDir.resolve("INDEX.adoc"),
            """
            |===
            | Borough | Project | DAG | Role in MVP0 | Session
            | BAKERY | bakery-gradle | N2 | Site statique | S001
            |===
            """.trimIndent(),
        )

        val newarkDir = tempDir.resolve("newark")
        Files.createDirectories(newarkDir)
        Files.writeString(
            newarkDir.resolve("INDEX.adoc"),
            """
            |===
            | Borough | Project | DAG | Role in MVP0 | Session
            | Newark | training-gradle | N2 | Pipeline formation | 010 ✅
            |===
            """.trimIndent(),
        )

        val data = crawler.crawlDirectory(tempDir)

        assertThat(data.boroughs).hasSize(2)
        assertThat(data.boroughs.map { it.name }).containsExactlyInAnyOrder("BAKERY", "Newark")
    }

    @Test
    fun `crawlDirectory should skip non-INDEX_adoc files`() {
        Files.writeString(tempDir.resolve("README.adoc"), "= Readme")
        val subDir = tempDir.resolve("sub")
        Files.createDirectories(subDir)
        Files.writeString(
            subDir.resolve("INDEX.adoc"),
            """
            |===
            | Borough | Project | DAG | Role in MVP0 | Session
            | Test | test-plugin | N0 | Testing | S000
            |===
            """.trimIndent(),
        )

        val data = crawler.crawlDirectory(tempDir)

        assertThat(data.boroughs).hasSize(1)
        assertThat(data.boroughs[0].name).isEqualTo("Test")
    }

    @Test
    fun `crawlSessionsHistory should parse session activities`() {
        val sessionsAdoc =
            """
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
        Files.writeString(
            dashboardDir.resolve("INDEX.adoc"),
            """
            |===
            | Borough | Project | DAG | Role in MVP0 | Session
            | Dashboard | dashboard-gradle | N3 | Dashboard vision | S003
            |===
            """.trimIndent(),
        )
        Files.writeString(
            dashboardDir.resolve("SESSIONS_HISTORY.adoc"),
            """
            |===
            | # | Date | Subject | Files
            | 003 | 2026-06-19 | Renommage ALGER | 15 fichiers
            |===
            """.trimIndent(),
        )

        val data = crawler.crawlDirectory(tempDir)

        assertThat(data.boroughs).hasSize(1)
        assertThat(data.sessions).hasSize(1)
        assertThat(data.sessions[0].borough).isEqualTo("Dashboard")
        assertThat(data.sessions[0].subject).isEqualTo("Renommage ALGER")
    }

    @Test
    fun `crawlDirectory should prune build output, caches and historical backups`() {
        val bakery = tempDir.resolve("bakery")
        Files.createDirectories(bakery)
        Files.writeString(
            bakery.resolve("INDEX.adoc"),
            """
            |===
            | Borough | Project | DAG | Role in MVP0 | Session
            | BAKERY | bakery-gradle | N2 | Site statique | S001
            |===
            """.trimIndent(),
        )
        for (skipped in listOf("build", ".git", ".gradle", "node_modules", ".agents/backup", ".agents/archives")) {
            val dir = bakery.resolve(skipped)
            Files.createDirectories(dir)
            Files.writeString(
                dir.resolve("INDEX.adoc"),
                """
                |===
                | Borough | Project | DAG | Role in MVP0 | Session
                | GHOST | ghost-gradle | N9 | build output | S999
                |===
                """.trimIndent(),
            )
            Files.writeString(dir.resolve("SESSIONS_HISTORY.adoc"), "|===\n| # | Date | Subject | Files\n| 999 | x | Ghost | 0 |\n|===")
        }

        val data = crawler.crawlDirectory(tempDir)

        assertThat(data.boroughs).hasSize(1)
        assertThat(data.boroughs[0].name).isEqualTo("BAKERY")
        assertThat(data.sessions).isEmpty()
    }

    @Test
    fun `crawlDirectory should deduplicate boroughs and epics across INDEX files`() {
        val rootIndex = tempDir.resolve("dashboard-gradle/INDEX.adoc")
        Files.createDirectories(rootIndex.parent)
        Files.writeString(
            rootIndex,
            """
            |===
            | Borough | Project | DAG | Role in MVP0 | Session
            | Dashboard | dashboard-gradle | N3 | Dashboard vision | S003
            |===
            |===
            | EPIC | Sujet | Pts | Priorite | Statut
            | DSH-0 | Bootstrap | 3 | P0 | ✅ S000
            |===
            """.trimIndent(),
        )

        val pluginIndex = tempDir.resolve("dashboard-gradle/dashboard-plugin/.agents/INDEX.adoc")
        Files.createDirectories(pluginIndex.parent)
        Files.writeString(
            pluginIndex,
            """
            |===
            | Borough | Project | DAG | Role in MVP0 | Session
            | Dashboard | dashboard-gradle | N3 | Dashboard vision | S005
            |===
            |===
            | EPIC | Sujet | Pts | Priorite | Statut
            | DSH-0 | Bootstrap | 3 | P0 | ✅ S000
            | DSH-9 | Serveur local | 8 | P1 | 🟡 S220
            |===
            """.trimIndent(),
        )

        val data = crawler.crawlDirectory(tempDir)

        assertThat(data.boroughs).hasSize(1)
        assertThat(data.epics).hasSize(2)
        assertThat(data.epics.map { it.id }).containsExactlyInAnyOrder("DSH-0", "DSH-9")
    }

    @Test
    fun `crawlDirectory should read point totals from a borough BACKLOG`() {
        val index = tempDir.resolve("foundry/dashboard/INDEX.adoc")
        Files.createDirectories(index.parent)
        Files.writeString(
            index,
            """
            |===
            | Borough | Project | DAG | Role in MVP0 | Session
            | Dashboard | dashboard-gradle | N3 | Vision | S004
            |===
            |===
            | EPIC | Description | Statut | Priorite
            | DSH-0 | Bootstrap | ✅ TERMINE | P0
            |===
            """.trimIndent(),
        )
        Files.writeString(
            tempDir.resolve("foundry/dashboard/BACKLOG.adoc"),
            """
            = BACKLOG
            |===
            | US | Description | Pts | Statut
            | US-1 | A | 5 | ✅ S-001
            | US-2 | B | 5 | ✅ S-002
            | US-3 | C | 5 | 🟡 À FAIRE
            |===
            """.trimIndent(),
        )

        val data = crawler.crawlDirectory(tempDir.resolve("foundry"))

        val dashboard = data.progress.byBorough.first { it.borough == "Dashboard" }
        assertThat(dashboard.donePoints).isEqualTo(10)
        assertThat(dashboard.totalPoints).isEqualTo(15)
        assertThat(dashboard.percent).isEqualTo(67)
    }

    @Test
    fun `crawlDirectory should parse the workspace backlog into transverse progress`() {
        val dashboardIndex = tempDir.resolve("foundry/dashboard/INDEX.adoc")
        Files.createDirectories(dashboardIndex.parent)
        Files.writeString(
            dashboardIndex,
            """
            |===
            | Borough | Project | DAG | Role in MVP0 | Session
            | Dashboard | dashboard-gradle | N3 | Vision | S003
            |===
            |===
            | EPIC | Sujet | Pts | Priorite | Statut
            | DSH-0 | Bootstrap | 6 | P0 | ✅ S000
            | DSH-1 | Rendu | 4 | P1 | 🟡 S001
            |===
            """.trimIndent(),
        )
        val backlog = tempDir.resolve("BACKLOG.adoc")
        Files.writeString(
            backlog,
            """
            = BACKLOG Global
            |===
            | EPIC | Sujet | Pts | Statut | Fichier source
            | GBL-001 | Termine | 10 | ✅ TERMINÉ | x
            | GBL-004 | Cadre | 10 | ✅ CADRÉ | y
            |===
            """.trimIndent(),
        )

        val data = crawler.crawlDirectory(tempDir.resolve("foundry"), backlog)

        assertThat(data.schemaVersion).isEqualTo("1.0")
        assertThat(data.boroughs.map { it.name }).contains("Transverse")
        assertThat(data.epics.map { it.id }).contains("GBL-001", "GBL-004")
        assertThat(data.progress.totalPoints).isEqualTo(30)
        assertThat(data.progress.donePoints).isEqualTo(16)
        assertThat(data.progress.percent).isEqualTo(53)
        assertThat(data.progress.byBorough.map { it.borough }).contains("Dashboard", "Transverse")
        val transverse = data.progress.byBorough.first { it.borough == "Transverse" }
        assertThat(transverse.donePoints).isEqualTo(10)
        assertThat(transverse.totalPoints).isEqualTo(20)
    }
}
