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

        val task = project.tasks.findByName("publishDashboardSite") as PublishDashboardTask
        assertThat(task.group).isEqualTo("dashboard")
        assertThat(task.description).isEqualTo("Publishes the generated dashboard site to the publish directory.")
    }

    @Test
    fun `task should expose outputDir as input and publishDir as output`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.register("publishDashboardSiteTest", PublishDashboardTask::class.java) { t ->
            t.outputDir.set(project.layout.buildDirectory.dir("dashboard"))
            t.publishDir.set(project.layout.buildDirectory.dir("dashboard-publish"))
        }.get()

        assertThat(task.inputs.hasInputs).isTrue
        assertThat(task.outputs.hasOutput).isTrue
    }

    @Test
    fun `task should copy generated dashboard JBake site to publish directory`() {
        val project = ProjectBuilder.builder().build()
        val outputDir = project.layout.buildDirectory.dir("dashboard").get().asFile.toPath()
        val publishDir = project.layout.buildDirectory.dir("dashboard-publish").get().asFile.toPath()
        val jbakeDir = outputDir.resolve("jbake")

        Files.createDirectories(jbakeDir.resolve("content"))
        Files.writeString(jbakeDir.resolve("jbake.properties"), "site.host=https://example.com/")
        Files.writeString(jbakeDir.resolve("content/index.html"), "<h1>Dashboard</h1>")

        val task = project.tasks.register("publishDashboardSite", PublishDashboardTask::class.java) { t ->
            t.outputDir.set(project.layout.buildDirectory.dir("dashboard"))
            t.publishDir.set(project.layout.buildDirectory.dir("dashboard-publish"))
        }.get()

        task.publish()

        assertThat(publishDir.resolve("jbake.properties")).exists()
        assertThat(publishDir.resolve("content/index.html")).exists()
        assertThat(Files.readString(publishDir.resolve("content/index.html"))).contains("Dashboard")
    }

    @Test
    fun `task should skip copy when output directory does not exist`() {
        val project = ProjectBuilder.builder().build()
        val publishDir = project.layout.buildDirectory.dir("dashboard-publish").get().asFile.toPath()

        val task = project.tasks.register("publishDashboardSite", PublishDashboardTask::class.java) { t ->
            t.outputDir.set(project.layout.buildDirectory.dir("missing-dashboard"))
            t.publishDir.set(project.layout.buildDirectory.dir("dashboard-publish"))
        }.get()

        task.publish()

        assertThat(publishDir).doesNotExist()
    }
}
