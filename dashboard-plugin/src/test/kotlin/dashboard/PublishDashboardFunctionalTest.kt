package dashboard

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class PublishDashboardFunctionalTest {

    @TempDir
    lateinit var testDir: Path

    private lateinit var foundryDir: Path

    @BeforeEach
    fun setUp() {
        foundryDir = testDir.resolve("foundry")
        Files.createDirectories(foundryDir)
    }

    @Test
    fun `publishDashboard should copy generated dashboard to publish directory`() {
        writePluginProject()
        writeEpicIndex(foundryDir.resolve("dashboard/INDEX.adoc"))

        val result = GradleRunner.create()
            .withProjectDir(testDir.toFile())
            .withArguments("publishDashboard", "--stacktrace")
            .withPluginClasspath()
            .build()

        assertThat(result.task(":publishDashboard")?.outcome).isIn(
            org.gradle.testkit.runner.TaskOutcome.SUCCESS,
            org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
        )
        assertThat(result.output).contains("Dashboard published")

        val publishDir = testDir.resolve("build/dashboard-publish")
        assertThat(publishDir.resolve("index.html")).exists()
        assertThat(publishDir.resolve("styles.css")).exists()
    }

    private fun writePluginProject() {
        Files.writeString(testDir.resolve("settings.gradle.kts"), "rootProject.name = \"test-dashboard\"")
        Files.writeString(testDir.resolve("build.gradle.kts"), """
            plugins {
                id("education.cccp.dashboard")
            }
            dashboard {
                configPath = "foundry"
                outputDir = "build/dashboard"
            }
        """.trimIndent())
    }

    private fun writeEpicIndex(path: Path) {
        Files.createDirectories(path.parent)
        Files.writeString(path, """
            |===
            | Borough | Project | DAG | Role in MVP0 | Session
            | Dashboard | dashboard-gradle | N3 | Vision | S004
            |===
            |===
            | EPIC | Sujet | Pts | Priorite | Statut
            | DSH-0 | Bootstrap | 3 | P0 | ✅ S000
            |===
        """.trimIndent())
    }
}
