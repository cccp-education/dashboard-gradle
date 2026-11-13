package dashboard

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class CrawlDashboardFunctionalTest {
    @TempDir
    lateinit var testDir: Path

    private lateinit var foundryDir: Path

    @BeforeEach
    fun setUp() {
        foundryDir = testDir.resolve("foundry")
        Files.createDirectories(foundryDir)
    }

    @Test
    fun `crawlDashboard should produce JSON output when foundry has INDEX_adoc files`() {
        writePluginProject()
        writeIndexAdoc(foundryDir.resolve("bakery/INDEX.adoc"), "BAKERY", "bakery-gradle", "N2", "Site statique")
        writeIndexAdoc(foundryDir.resolve("newark/INDEX.adoc"), "Newark", "training-gradle", "N2", "Pipeline")

        val result =
            GradleRunner
                .create()
                .withProjectDir(testDir.toFile())
                .withArguments("crawlDashboard", "--stacktrace")
                .withPluginClasspath()
                .build()

        assertThat(result.output).contains("Dashboard data written to")
        assertThat(result.output).contains("2 boroughs")
        assertThat(result.task(":crawlDashboard")?.outcome).isIn(
            org.gradle.testkit.runner.TaskOutcome.SUCCESS,
            org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE,
        )

        val jsonFile = testDir.resolve("build/dashboard-data/dashboard-data.json")
        assertThat(jsonFile).exists()
        val content = Files.readString(jsonFile)
        assertThat(content).contains("BAKERY")
        assertThat(content).contains("Newark")
    }

    @Test
    fun `crawlDashboard should expose schemaVersion and point progress`() {
        writePluginProjectWithBacklog()
        writeIndexAdoc(foundryDir.resolve("bakery/INDEX.adoc"), "BAKERY", "bakery-gradle", "N2", "Site statique")
        Files.writeString(
            testDir.resolve("BACKLOG.adoc"),
            """
            = BACKLOG Global
            |===
            | EPIC | Sujet | Pts | Statut | Fichier source
            | GBL-001 | Termine | 5 | ✅ TERMINÉ | x
            | GBL-002 | Planifie | 5 | 🔲 CADRÉ | y
            |===
            """.trimIndent(),
        )

        GradleRunner
            .create()
            .withProjectDir(testDir.toFile())
            .withArguments("crawlDashboard", "--stacktrace")
            .withPluginClasspath()
            .build()

        val json = Files.readString(testDir.resolve("build/dashboard-data/dashboard-data.json"))
        assertThat(json).contains("\"schemaVersion\" : \"1.0\"")
        assertThat(json).contains("\"progress\"")
        assertThat(json).contains("\"Transverse\"")
        assertThat(json).contains("\"donePoints\" : 5")
    }

    @Test
    fun `crawlDashboard should warn when config path does not exist`() {
        writePluginProjectWithCustomConfig("nonexistent")

        val result =
            GradleRunner
                .create()
                .withProjectDir(testDir.toFile())
                .withArguments("crawlDashboard", "--stacktrace")
                .withPluginClasspath()
                .build()

        assertThat(result.output).contains("Config path does not exist")
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

    private fun writePluginProjectWithBacklog() {
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
                workspaceBacklog = "BACKLOG.adoc"
            }
            """.trimIndent(),
        )
    }

    private fun writePluginProjectWithCustomConfig(configPath: String) {
        Files.writeString(testDir.resolve("settings.gradle.kts"), "rootProject.name = \"test-dashboard\"")
        Files.writeString(
            testDir.resolve("build.gradle.kts"),
            """
            plugins {
                id("education.cccp.dashboard")
            }
            dashboard {
                configPath = "$configPath"
                outputDir = "build/dashboard"
            }
            """.trimIndent(),
        )
    }

    private fun writeIndexAdoc(
        path: Path,
        borough: String,
        project: String,
        dag: String,
        role: String,
    ) {
        Files.createDirectories(path.parent)
        Files.writeString(
            path,
            """
            |===
            | Borough | Project | DAG | Role in MVP0 | Session
            | $borough | $project | $dag | $role | S001
            |===
            """.trimIndent(),
        )
    }
}
