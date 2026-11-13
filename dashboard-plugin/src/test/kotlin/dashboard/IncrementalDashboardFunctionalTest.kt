package dashboard

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

/**
 * DSH-BOUNDARY-3 — `crawlDashboard` / `generateDashboard` are incremental:
 * they are UP-TO-DATE until a governance file (or the pivot) changes.
 */
class IncrementalDashboardFunctionalTest {
    @TempDir
    lateinit var testDir: Path

    @BeforeEach
    fun setUp() {
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
        writeIndex("Dashboard", "DSH-0 | Bootstrap | 3 | P0 | ✅ S000")
    }

    @Test
    fun `crawlDashboard should be up to date until a governance file changes`() {
        assertThat(run("crawlDashboard").task(":crawlDashboard")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(run("crawlDashboard").task(":crawlDashboard")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)

        writeIndex("Dashboard", "DSH-1 | Serve | 8 | P1 | ✅ S220")

        assertThat(run("crawlDashboard").task(":crawlDashboard")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(run("crawlDashboard").task(":crawlDashboard")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
        assertThat(testDir.resolve("build/dashboard-data/dashboard-data.json")).exists()
    }

    @Test
    fun `generateDashboard should be up to date when the pivot is unchanged`() {
        assertThat(run("generateDashboard").task(":generateDashboard")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

        val second = run("generateDashboard")
        assertThat(second.task(":crawlDashboard")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
        assertThat(second.task(":generateDashboard")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
        assertThat(testDir.resolve("build/dashboard/index.html")).exists()

        val forced = run("generateDashboard", "--rerun-tasks")
        assertThat(forced.task(":generateDashboard")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    private fun run(vararg arguments: String): BuildResult =
        GradleRunner
            .create()
            .withProjectDir(testDir.toFile())
            .withArguments(*arguments, "--stacktrace")
            .withPluginClasspath()
            .build()

    private fun writeIndex(
        borough: String,
        epicRow: String,
    ) {
        val index = testDir.resolve("foundry/${borough.lowercase()}/INDEX.adoc")
        Files.createDirectories(index.parent)
        Files.writeString(
            index,
            """
            |===
            | Borough | Project | DAG | Role in MVP0 | Session
            | $borough | dashboard-gradle | N3 | Vision | S004
            |===
            |===
            | EPIC | Sujet | Pts | Priorite | Statut
            | $epicRow
            |===
            """.trimIndent(),
        )
    }
}
