package dashboard

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class DashboardPluginTest {

    @Test
    fun `plugin should register extension and tasks`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("education.cccp.dashboard")

        assertThat(project.extensions.findByName("dashboard")).isInstanceOf(DashboardExtension::class.java)

        assertThat(project.tasks.findByName("crawlDashboard")).isNotNull
        assertThat(project.tasks.findByName("generateDashboard")).isNotNull
        assertThat(project.tasks.findByName("publishDashboard")).isNotNull
    }

    @Test
    fun `tasks should belong to dashboard group`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("education.cccp.dashboard")

        val crawl = project.tasks.findByName("crawlDashboard")
        assertThat(crawl).isNotNull
        assertThat(crawl!!.group).isEqualTo("dashboard")
        assertThat(crawl.description).isEqualTo("Crawls INDEX.adoc and SESSIONS_HISTORY.adoc from all boroughs.")

        val generate = project.tasks.findByName("generateDashboard")
        assertThat(generate).isNotNull
        assertThat(generate!!.group).isEqualTo("dashboard")
        assertThat(generate.description).isEqualTo("Generates the static dashboard site from crawled data.")
    }

    @Test
    fun `extension should have default values`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("education.cccp.dashboard")

        val ext = project.extensions.findByName("dashboard") as DashboardExtension
        assertThat(ext.configPath.get()).isEqualTo("foundry")
        assertThat(ext.outputDir.get()).isEqualTo("build/dashboard")
        assertThat(ext.publishDir.get()).isEqualTo("build/dashboard-publish")
        assertThat(ext.boroughs.get()).isEmpty()
    }

    @Test
    fun `extension should accept custom values`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("education.cccp.dashboard")

        val ext = project.extensions.findByName("dashboard") as DashboardExtension
        ext.configPath.set("custom/config")
        ext.outputDir.set("custom/output")
        ext.publishDir.set("custom/publish")
        ext.boroughs.set(listOf("BAKERY", "NEWARK"))

        assertThat(ext.configPath.get()).isEqualTo("custom/config")
        assertThat(ext.outputDir.get()).isEqualTo("custom/output")
        assertThat(ext.publishDir.get()).isEqualTo("custom/publish")
        assertThat(ext.boroughs.get()).containsExactly("BAKERY", "NEWARK")
    }
}
