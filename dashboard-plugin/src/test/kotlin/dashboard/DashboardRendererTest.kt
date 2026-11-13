package dashboard

import dashboard.CriticalPathRing
import dashboard.model.BoroughData
import dashboard.model.BoroughProgress
import dashboard.model.DashboardData
import dashboard.model.EpicData
import dashboard.model.EpicStatus
import dashboard.model.ProgressSummary
import dashboard.model.SessionActivity
import dashboard.render.DashboardRenderer
import dashboard.render.Slug
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class DashboardRendererTest {
    @Test
    fun `render should produce a panorama with the three zones`(
        @TempDir tempDir: Path,
    ) {
        DashboardRenderer().render(sampleData(), tempDir)

        val html = Files.readString(tempDir.resolve("index.html"))
        assertThat(html).contains("Vision — chaîne MVP0")
        assertThat(html).contains("Avancement")
        assertThat(html).contains("Structure")
        assertThat(html).contains("Newark")
        assertThat(html).contains("BAKERY")
        assertThat(html).contains("Corpus metier")
    }

    @Test
    fun `render should generate one borough sheet per borough`(
        @TempDir tempDir: Path,
    ) {
        DashboardRenderer().render(sampleData(), tempDir)

        assertThat(tempDir.resolve("borough-newark.html")).exists()
        assertThat(tempDir.resolve("borough-bakery.html")).exists()

        val bakery = Files.readString(tempDir.resolve("borough-bakery.html"))
        assertThat(bakery).contains("EPICs")
        assertThat(bakery).contains("BKY-1")

        val newark = Files.readString(tempDir.resolve("borough-newark.html"))
        assertThat(newark).contains("DSH-0")
        assertThat(newark).contains("DSH-2")
    }

    @Test
    fun `render should include recent sessions on the borough sheet`(
        @TempDir tempDir: Path,
    ) {
        val data = sampleData()
        DashboardRenderer().render(data, tempDir)

        val html = Files.readString(tempDir.resolve("borough-dashboard.html"))
        assertThat(html).contains("Sessions récentes")
        assertThat(html).contains("Cadrage DSH-3")
        assertThat(html).contains("2026-06-22")
    }

    @Test
    fun `render should include milestones in the vision zone`(
        @TempDir tempDir: Path,
    ) {
        DashboardRenderer().render(sampleData(), tempDir)

        val html = Files.readString(tempDir.resolve("index.html"))
        assertThat(html).contains("Jalons MVP0")
        assertThat(html).contains("2026-06-18")
        assertThat(html).contains("2026-06-23")
    }

    @Test
    fun `render should copy the local assets`(
        @TempDir tempDir: Path,
    ) {
        DashboardRenderer().render(sampleData(), tempDir)

        assertThat(tempDir.resolve("styles.css")).exists()
        assertThat(tempDir.resolve("vendor/bootstrap.min.css")).exists()
        assertThat(tempDir.resolve("dashboard-data.json")).exists()
    }

    @Test
    fun `render should expose the structure without any external asset`(
        @TempDir tempDir: Path,
    ) {
        DashboardRenderer().render(sampleData(), tempDir)

        val html = Files.readString(tempDir.resolve("index.html"))
        assertThat(html).contains("Structure")
        assertThat(html).contains("N2")
        assertThat(html).contains("N3")
        assertThat(html).contains("borough-newark.html")

        // DSH-BOUNDARY — offline render: no CDN, no interactive canvas.
        assertThat(html).doesNotContain("cdn.jsdelivr")
        assertThat(html).doesNotContain("unpkg.com")
        assertThat(html).doesNotContain("vis-network")
        assertThat(html).doesNotContain("src=\"http")
        assertThat(html).doesNotContain("href=\"http")
        assertThat(html).contains("vendor/bootstrap.min.css")
    }

    @Test
    fun `render should display global and per borough point progress`(
        @TempDir tempDir: Path,
    ) {
        val data =
            DashboardData(
                boroughs = listOf(BoroughData("Dashboard", "dashboard-gradle", "N3", "Vision", "S005")),
                epics =
                    listOf(
                        EpicData("DSH-0", "Bootstrap", "Dashboard", 8, "P0", EpicStatus.TERMINE),
                        EpicData("DSH-1", "Serve", "Dashboard", 4, "P1", EpicStatus.EN_COURS),
                    ),
                dagNodes = emptyList(),
                progress =
                    ProgressSummary(
                        donePoints = 8,
                        totalPoints = 12,
                        percent = 67,
                        doneEpics = 1,
                        totalEpics = 2,
                        byBorough = listOf(BoroughProgress("Dashboard", "dashboard-gradle", 8, 12, 67, 1, 2)),
                    ),
            )

        DashboardRenderer().render(data, tempDir)

        val html = Files.readString(tempDir.resolve("index.html"))
        assertThat(html).contains("67 %")
        assertThat(html).contains("8 / 12 pts")
        val borough = Files.readString(tempDir.resolve("borough-dashboard.html"))
        assertThat(borough).contains("67 %")
        assertThat(borough).contains("8 / 12 pts")
    }

    @Test
    fun `slug should be deterministic and accent free`() {
        assertThat(Slug.of("Détroit")).isEqualTo("detroit")
        assertThat(Slug.of("New Orleans")).isEqualTo("new-orleans")
        assertThat(Slug.of("workspace-agent (IntelliJ)")).isEqualTo("workspace-agent-intellij")
        assertThat(Slug.fileName("Bushwick 🟤")).isEqualTo("borough-bushwick.html")
    }

    @Test
    fun `epic status should map to css class`() {
        assertThat(DashboardRenderer.statusCssClass(EpicStatus.TERMINE)).isEqualTo("status-done")
        assertThat(DashboardRenderer.statusCssClass(EpicStatus.EN_COURS)).isEqualTo("status-in-progress")
        assertThat(DashboardRenderer.statusCssClass(EpicStatus.PLANIFIE)).isEqualTo("status-planned")
        assertThat(DashboardRenderer.statusCssClass(EpicStatus.BLOQUE)).isEqualTo("status-blocked")
    }

    private fun sampleData(): DashboardData =
        DashboardData(
            boroughs =
                listOf(
                    BoroughData("Newark", "training-gradle", "N2", "Pipeline", "010"),
                    BoroughData("BAKERY", "bakery-gradle", "N2", "Site statique", "S001"),
                    BoroughData("Dashboard", "dashboard-gradle", "N3", "Vision", "S004"),
                ),
            epics =
                listOf(
                    EpicData("DSH-0", "Bootstrap", "Dashboard", 3, "P0", EpicStatus.TERMINE, "000"),
                    EpicData("DSH-2", "Crawler", "Dashboard", 13, "P0", EpicStatus.PLANIFIE, "002"),
                    EpicData("BKY-1", "Bakery task", "BAKERY", 5, "P1", EpicStatus.EN_COURS, "001"),
                    EpicData("DSH-7", "Integration", "Dashboard", 5, "P1", EpicStatus.EN_COURS, "008"),
                ),
            dagNodes = emptyList(),
            criticalPath = listOf(CriticalPathRing("A1", "Corpus metier", listOf("training-gradle"))),
            sessions =
                listOf(
                    SessionActivity("004", "2026-06-22", "vision", "Cadrage DSH-3", "Dashboard", "4 fichiers"),
                    SessionActivity("000", "2026-06-18", "gouvernance", "Bootstrap", "Dashboard", ""),
                    SessionActivity("008", "2026-06-23", "implementation", "Integration", "Dashboard", ""),
                ),
        )
}
