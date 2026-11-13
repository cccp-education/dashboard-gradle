package dashboard

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ServeDashboardFunctionalTest {
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
        val dashboardDir = testDir.resolve("foundry/dashboard")
        Files.createDirectories(dashboardDir)
        Files.writeString(
            dashboardDir.resolve("INDEX.adoc"),
            """
            |===
            | Borough | Project | DAG | Role in MVP0 | Session
            | Dashboard | dashboard-gradle | N3 | Dashboard | S001
            |===
            """.trimIndent(),
        )
    }

    @Test
    fun `serve is registered in the dashboard group`() {
        val result =
            GradleRunner
                .create()
                .withProjectDir(testDir.toFile())
                .withArguments("tasks", "--all")
                .withPluginClasspath()
                .build()

        assertThat(result.output).contains("serve - Serves the generated dashboard locally (default http://localhost:8820")
    }

    @Test
    fun `serve depends on generateDashboard`() {
        val result =
            GradleRunner
                .create()
                .withProjectDir(testDir.toFile())
                .withArguments("serve", "--dry-run")
                .withPluginClasspath()
                .build()

        assertThat(result.output).contains(":generateDashboard")
        assertThat(result.output).contains(":serve")
    }
}
