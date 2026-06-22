package education.cccp.dashboard.scenarios

import io.cucumber.java.After
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import java.nio.file.Files
import java.nio.file.Path

class DashboardWorld {
    lateinit var projectDir: Path
    var buildResult: BuildResult? = null
    private var configPath: String = "foundry"

    fun createGradleProject() {
        projectDir = Files.createTempDirectory("dashboard-test-")
        Files.writeString(projectDir.resolve("settings.gradle.kts"), "rootProject.name = \"${projectDir.fileName}\"")
        val configLine = if (configPath == "foundry") "" else """dashboard { configPath = "$configPath" }"""
        Files.writeString(projectDir.resolve("build.gradle.kts"), """
            plugins { id("education.cccp.dashboard") }
            $configLine
        """.trimIndent())
    }

    fun executeGradle(vararg args: String): BuildResult {
        return try {
            GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withArguments(*args, "--stacktrace")
                .withPluginClasspath()
                .build()
                .also { buildResult = it }
        } catch (e: UnexpectedBuildFailure) {
            e.buildResult.also { buildResult = it }
        }
    }

    fun writeIndexAdoc(path: String, content: String) {
        val file = projectDir.resolve(path)
        Files.createDirectories(file.parent)
        Files.writeString(file, content)
    }

    fun readJsonOutput(): String? {
        val jsonFile = projectDir.resolve("build/dashboard/dashboard-data.json")
        return if (Files.exists(jsonFile)) Files.readString(jsonFile) else null
    }

    fun readHtmlOutput(): String? {
        val htmlFile = projectDir.resolve("build/dashboard/index.html")
        return if (Files.exists(htmlFile)) Files.readString(htmlFile) else null
    }

    fun setConfigPath(path: String) {
        configPath = path
    }

    @After
    fun cleanup() {
        if (::projectDir.isInitialized) {
            projectDir.toFile().deleteRecursively()
        }
        buildResult = null
    }
}
