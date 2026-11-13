package dashboard

import dashboard.model.BoroughData
import dashboard.model.DashboardData
import dashboard.model.EpicData
import dashboard.model.EpicStatus
import dashboard.model.SessionActivity
import dashboard.render.DashboardRenderer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class DashboardRendererTest {
    @Test
    fun `render should produce index html with epic matrix`(
        @TempDir tempDir: Path,
    ) {
        val data =
            DashboardData(
                boroughs =
                    listOf(
                        BoroughData("Newark", "training-gradle", "N2", "Pipeline", "010 ✅"),
                        BoroughData("BAKERY", "bakery-gradle", "N2", "Site statique", "S001"),
                    ),
                epics =
                    listOf(
                        EpicData("DSH-0", "Bootstrap", "Newark", 3, "P0", EpicStatus.TERMINE),
                        EpicData("DSH-1", "Plugin scaffold", "BAKERY", 8, "P0", EpicStatus.EN_COURS),
                        EpicData("DSH-2", "Crawler", "Newark", 13, "P0", EpicStatus.PLANIFIE),
                    ),
                dagNodes = emptyList(),
            )

        val renderer = DashboardRenderer()
        renderer.render(data, tempDir)

        val indexHtml = tempDir.resolve("index.html")
        assertThat(indexHtml).exists()
        val html = Files.readString(indexHtml)
        assertThat(html).contains("Dashboard")
        assertThat(html).contains("Newark")
        assertThat(html).contains("BAKERY")
        assertThat(html).contains("DSH-0")
        assertThat(html).contains("TERMINE")
        assertThat(html).contains("EN_COURS")
    }

    @Test
    fun `render should group epics by borough in html table`(
        @TempDir tempDir: Path,
    ) {
        val data =
            DashboardData(
                boroughs =
                    listOf(
                        BoroughData("BAKERY", "bakery-gradle", "N2", "Site statique", "S001"),
                        BoroughData("Dashboard", "dashboard-gradle", "N3", "Vision", "S005"),
                    ),
                epics =
                    listOf(
                        EpicData("BKY-1", "Bakery task", "BAKERY", 5, "P1", EpicStatus.EN_COURS),
                        EpicData("DSH-0", "Bootstrap", "Dashboard", 3, "P0", EpicStatus.TERMINE),
                    ),
                dagNodes = emptyList(),
            )

        DashboardRenderer().render(data, tempDir)

        val html = Files.readString(tempDir.resolve("index.html"))
        assertThat(html).contains("BAKERY (N2)")
        assertThat(html).contains("Dashboard (N3)")
        assertThat(html).contains("bakery-gradle")
        assertThat(html).contains("BKY-1")
        assertThat(html).contains("DSH-0")
    }

    @Test
    fun `render should include activity stream section when sessions present`(
        @TempDir tempDir: Path,
    ) {
        val data =
            DashboardData(
                boroughs =
                    listOf(
                        BoroughData("Dashboard", "dashboard-gradle", "N3", "Vision", "S004"),
                    ),
                epics = emptyList(),
                dagNodes = emptyList(),
                sessions =
                    listOf(
                        SessionActivity("004", "2026-06-22", "vision", "Cadrage DSH-3", "Dashboard", "4 fichiers"),
                    ),
            )

        val renderer = DashboardRenderer()
        renderer.render(data, tempDir)

        val html = Files.readString(tempDir.resolve("index.html"))
        assertThat(html).contains("Activity Stream")
        assertThat(html).contains("Cadrage DSH-3")
        assertThat(html).contains("2026-06-22")
    }

    @Test
    fun `render should include timeline section with milestone entries`(
        @TempDir tempDir: Path,
    ) {
        val data =
            DashboardData(
                boroughs =
                    listOf(
                        BoroughData("Dashboard", "dashboard-gradle", "N3", "Vision", "S008"),
                    ),
                epics =
                    listOf(
                        EpicData("DSH-0", "Bootstrap", "Dashboard", 3, "P0", EpicStatus.TERMINE, "000"),
                        EpicData("DSH-7", "Integration", "Dashboard", 5, "P1", EpicStatus.EN_COURS, "008"),
                    ),
                dagNodes = emptyList(),
                sessions =
                    listOf(
                        SessionActivity("000", "2026-06-18", "gouvernance", "Bootstrap", "Dashboard", ""),
                        SessionActivity("008", "2026-06-23", "implementation", "Integration", "Dashboard", ""),
                    ),
            )

        DashboardRenderer().render(data, tempDir)

        val html = Files.readString(tempDir.resolve("index.html"))
        assertThat(html).contains("Timeline")
        assertThat(html).contains("2026-06-18")
        assertThat(html).contains("2026-06-23")
        assertThat(html).contains("Bootstrap")
    }

    @Test
    fun `render should copy dashboard css to output directory`(
        @TempDir tempDir: Path,
    ) {
        val data =
            DashboardData(
                boroughs = listOf(BoroughData("X", "x-gradle", "N0", "Test", "S000")),
                epics = emptyList(),
                dagNodes = emptyList(),
            )

        DashboardRenderer().render(data, tempDir)

        assertThat(tempDir.resolve("styles.css")).exists()
        assertThat(tempDir.resolve("vendor/bootstrap.min.css")).exists()
    }

    @Test
    fun `render should expose the DAG as a static table without any external asset`(
        @TempDir tempDir: Path,
    ) {
        val data =
            DashboardData(
                boroughs =
                    listOf(
                        BoroughData("BAKERY", "bakery-gradle", "N2", "Site", "S001"),
                        BoroughData("Dashboard", "dashboard-gradle", "N3", "Vision", "S005"),
                    ),
                epics = emptyList(),
                dagNodes = emptyList(),
            )

        DashboardRenderer().render(data, tempDir)

        val html = Files.readString(tempDir.resolve("index.html"))
        assertThat(html).contains("Couches DAG")
        assertThat(html).contains("BAKERY (N2)")
        assertThat(html).contains("Dashboard (N3)")
        assertThat(html).contains("bakery-gradle")

        // DSH-9.2 — offline render: no CDN, no interactive canvas, local assets only.
        assertThat(html).doesNotContain("cdn.jsdelivr")
        assertThat(html).doesNotContain("unpkg.com")
        assertThat(html).doesNotContain("vis-network")
        assertThat(html).doesNotContain("src=\"http")
        assertThat(html).doesNotContain("href=\"http")
        assertThat(html).contains("vendor/bootstrap.min.css")
    }

    @Test
    fun `epic status should map to css class`() {
        assertThat(DashboardRenderer.statusCssClass(EpicStatus.TERMINE)).isEqualTo("status-done")
        assertThat(DashboardRenderer.statusCssClass(EpicStatus.EN_COURS)).isEqualTo("status-in-progress")
        assertThat(DashboardRenderer.statusCssClass(EpicStatus.PLANIFIE)).isEqualTo("status-planned")
        assertThat(DashboardRenderer.statusCssClass(EpicStatus.BLOQUE)).isEqualTo("status-blocked")
    }
}
