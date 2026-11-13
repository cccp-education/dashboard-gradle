package dashboard

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class GenerateDashboardFunctionalTest {
    @TempDir
    lateinit var testDir: Path

    private lateinit var foundryDir: Path

    @BeforeEach
    fun setUp() {
        foundryDir = testDir.resolve("foundry")
        Files.createDirectories(foundryDir)
    }

    @Test
    fun `generateDashboard should produce the panorama and a borough sheet`() {
        writePluginProject()
        writeEpicIndex(foundryDir.resolve("dashboard/INDEX.adoc"))

        val result =
            GradleRunner
                .create()
                .withProjectDir(testDir.toFile())
                .withArguments("generateDashboard", "--stacktrace")
                .withPluginClasspath()
                .build()

        assertThat(result.task(":generateDashboard")?.outcome).isIn(
            org.gradle.testkit.runner.TaskOutcome.SUCCESS,
            org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE,
        )
        assertThat(result.output).contains("Dashboard site generated")

        val indexHtml = testDir.resolve("build/dashboard/index.html")
        assertThat(indexHtml).exists()
        val html = Files.readString(indexHtml)
        assertThat(html).contains("Dashboard Workspace CCCP")
        assertThat(html).contains("Vision — chaîne MVP0")
        assertThat(html).contains("Avancement")
        assertThat(html).contains("Structure")

        val boroughHtml = testDir.resolve("build/dashboard/borough-dashboard.html")
        assertThat(boroughHtml).exists()
        val borough = Files.readString(boroughHtml)
        assertThat(borough).contains("EPICs")
        assertThat(borough).contains("DSH-0")
        assertThat(borough).contains("status-done")

        assertThat(testDir.resolve("build/dashboard/styles.css")).exists()
        assertThat(testDir.resolve("build/dashboard/vendor/bootstrap.min.css")).exists()
    }

    @Test
    fun `generateDashboard should include recent sessions on the borough sheet`() {
        writePluginProject()
        val dashboardDir = foundryDir.resolve("dashboard")
        Files.createDirectories(dashboardDir)
        Files.writeString(
            dashboardDir.resolve("INDEX.adoc"),
            """
            |===
            | Borough | Project | DAG | Role in MVP0 | Session
            | Dashboard | dashboard-gradle | N3 | Vision | S004
            |===
            """.trimIndent(),
        )
        Files.writeString(
            dashboardDir.resolve("SESSIONS_HISTORY.adoc"),
            """
            |===
            | # | Date | Objet | Fichiers
            | 004 | 2026-06-22 | Cadrage DSH-3 | 4 fichiers
            |===
            """.trimIndent(),
        )

        GradleRunner
            .create()
            .withProjectDir(testDir.toFile())
            .withArguments("generateDashboard", "--stacktrace")
            .withPluginClasspath()
            .build()

        val borough = Files.readString(testDir.resolve("build/dashboard/borough-dashboard.html"))
        assertThat(borough).contains("Sessions récentes")
        assertThat(borough).contains("Cadrage DSH-3")
    }

    private fun writePluginProject() {
        Files.writeString(testDir.resolve("settings.gradle.kts"), "rootProject.name = \"test-dashboard\"")
        Files.writeString(
            testDir.resolve("build.gradle.kts"),
            """
            plugins {
                id("education.cccp.dashboard")
            }
            dashboard {
                configPath = "foundry"
                outputDir = "build/dashboard"
            }
            """.trimIndent(),
        )
    }

    private fun writeEpicIndex(path: Path) {
        Files.createDirectories(path.parent)
        Files.writeString(
            path,
            """
            |===
            | Borough | Project | DAG | Role in MVP0 | Session
            | Dashboard | dashboard-gradle | N3 | Vision | S004
            |===
            |===
            | EPIC | Sujet | Pts | Priorite | Statut
            | DSH-0 | Bootstrap | 3 | P0 | ✅ S000
            | DSH-1 | Plugin scaffold | 8 | P0 | 🔄 S001
            | DSH-2 | Crawler | 13 | P0 | PLANIFIE
            |===
            """.trimIndent(),
        )
    }
}
