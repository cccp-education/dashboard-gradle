package dashboard.publish

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import java.nio.file.Files

class PublishDashboardTaskTest {

    @Test
    fun `task should belong to dashboard group and have description`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("education.cccp.dashboard")

        val task = project.tasks.findByName("publishDashboard") as PublishDashboardTask
        assertThat(task.group).isEqualTo("dashboard")
        assertThat(task.description).isEqualTo("Publishes the generated dashboard site to the publish directory.")
    }

    @Test
    fun `task should expose outputDir as input and publishDir as output`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.register("publishDashboardTest", PublishDashboardTask::class.java) { t ->
            t.outputDir.set(project.layout.buildDirectory.dir("dashboard"))
            t.publishDir.set(project.layout.buildDirectory.dir("dashboard-publish"))
        }.get()

        assertThat(task.inputs.hasInputs).isTrue
        assertThat(task.outputs.hasOutput).isTrue
    }

    @Test
    fun `task should copy generated dashboard output to publish directory`() {
        val project = ProjectBuilder.builder().build()
        val outputDir = project.layout.buildDirectory.dir("dashboard").get().asFile.toPath()
        val publishDir = project.layout.buildDirectory.dir("dashboard-publish").get().asFile.toPath()

        Files.createDirectories(outputDir)
        Files.writeString(outputDir.resolve("index.html"), "<h1>Dashboard</h1>")
        Files.writeString(outputDir.resolve("styles.css"), "body {}")

        val task = project.tasks.register("publishDashboard", PublishDashboardTask::class.java) { t ->
            t.outputDir.set(project.layout.buildDirectory.dir("dashboard"))
            t.publishDir.set(project.layout.buildDirectory.dir("dashboard-publish"))
        }.get()

        task.publish()

        assertThat(publishDir.resolve("index.html")).exists()
        assertThat(publishDir.resolve("styles.css")).exists()
        assertThat(Files.readString(publishDir.resolve("index.html"))).contains("Dashboard")
    }

    @Test
    fun `task should skip copy when output directory does not exist`() {
        val project = ProjectBuilder.builder().build()
        val publishDir = project.layout.buildDirectory.dir("dashboard-publish").get().asFile.toPath()

        val task = project.tasks.register("publishDashboard", PublishDashboardTask::class.java) { t ->
            t.outputDir.set(project.layout.buildDirectory.dir("missing-dashboard"))
            t.publishDir.set(project.layout.buildDirectory.dir("dashboard-publish"))
        }.get()

        task.publish()

        assertThat(publishDir).doesNotExist()
    }
}
