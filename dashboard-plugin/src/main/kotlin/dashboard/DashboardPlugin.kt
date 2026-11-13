package dashboard

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dashboard.publish.PublishDashboardTask
import dashboard.render.DashboardRenderer
import dashboard.serve.ServeDashboardTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.nio.file.Files

/**
 * Gradle plugin entry point for the workspace dashboard.
 *
 * Registers four tasks:
 * - `crawlDashboard`: scans INDEX.adoc / SESSIONS_HISTORY.adoc and writes dashboard-data.json
 * - `generateDashboard`: renders the static HTML/CSS dashboard from the JSON data
 * - `publishDashboardSite`: copies the generated site to the configured publish directory
 * - `serve`: serves the generated site locally (loopback only) until interrupted
 */
class DashboardPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("dashboard", DashboardExtension::class.java)

        extension.serveHost.convention(
            project.providers.gradleProperty("dashboard.serveHost").orElse("127.0.0.1"),
        )
        extension.servePort.convention(
            project.providers
                .gradleProperty("dashboard.servePort")
                .map { it.trim().toInt() }
                .orElse(8080),
        )

        project.tasks.register("crawlDashboard") { task ->
            task.group = DashboardConstants.DASHBOARD_GROUP
            task.description = "Crawls INDEX.adoc and SESSIONS_HISTORY.adoc from all boroughs."
            task.doLast {
                val configPath = extension.configPath.get()
                val outputDir = extension.outputDir.get()
                project.logger.lifecycle("Crawling dashboard from $configPath → $outputDir")

                val root = project.projectDir.toPath().resolve(configPath)
                if (!Files.exists(root)) {
                    project.logger.warn("Config path does not exist: $root")
                    return@doLast
                }

                val crawler = IndexCrawler()
                val data = crawler.crawlDirectory(root)

                val outPath = project.projectDir.toPath().resolve(outputDir)
                Files.createDirectories(outPath)

                val mapper =
                    jacksonObjectMapper().apply {
                        enable(SerializationFeature.INDENT_OUTPUT)
                    }

                val jsonFile = outPath.resolve("dashboard-data.json")
                mapper.writeValue(jsonFile.toFile(), data)
                project.logger.lifecycle(
                    "Dashboard data written to $jsonFile (${data.boroughs.size} boroughs, ${data.epics.size} epics, ${data.sessions.size} sessions)",
                )
            }
        }

        project.tasks.register("generateDashboard") { task ->
            task.group = DashboardConstants.DASHBOARD_GROUP
            task.description = "Generates the static dashboard site from crawled data."
            task.dependsOn("crawlDashboard")
            task.doLast {
                val outputDir = project.projectDir.toPath().resolve(extension.outputDir.get())
                val jsonFile = outputDir.resolve("dashboard-data.json")

                if (!Files.exists(jsonFile)) {
                    project.logger.warn("Dashboard data not found: $jsonFile. Run crawlDashboard first.")
                    return@doLast
                }

                project.logger.lifecycle("Generating dashboard site at $outputDir")
                DashboardRenderer().renderFromJson(jsonFile, outputDir)
                project.logger.lifecycle("Dashboard site generated: ${outputDir.resolve("index.html")}")
            }
        }

        project.tasks.register("publishDashboardSite", PublishDashboardTask::class.java) { task ->
            task.dependsOn("generateDashboard")
            task.outputDir.set(project.layout.projectDirectory.dir(extension.outputDir))
            task.publishDir.set(project.layout.projectDirectory.dir(extension.publishDir))
        }

        project.tasks.register("serve", ServeDashboardTask::class.java) { task ->
            task.group = DashboardConstants.DASHBOARD_GROUP
            task.description =
                "Serves the generated dashboard locally (default http://127.0.0.1:8080, Ctrl+C to stop)."
            task.dependsOn("generateDashboard")
            task.outputDir.set(project.layout.projectDirectory.dir(extension.outputDir))
            task.host.set(extension.serveHost)
            task.port.set(extension.servePort)
        }
    }
}
