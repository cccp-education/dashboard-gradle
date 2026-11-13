package dashboard

import dashboard.publish.PublishDashboardTask
import dashboard.serve.ServeDashboardTask
import dashboard.tasks.CrawlDashboardTask
import dashboard.tasks.GenerateDashboardTask
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Gradle plugin entry point for the workspace dashboard.
 *
 * Registers four tasks:
 * - `crawlDashboard`: scans the workspace governance files and writes the JSON pivot
 * - `generateDashboard`: renders the static Panorama + borough sheets from the pivot
 * - `publishDashboardSite`: copies the generated site to the configured publish directory
 * - `serve`: serves the generated site locally (loopback only) until interrupted
 */
class DashboardPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("dashboard", DashboardExtension::class.java)

        extension.serveHost.convention(
            project.providers.gradleProperty("dashboard.serveHost").orElse("localhost"),
        )
        extension.servePort.convention(
            project.providers
                .gradleProperty("dashboard.servePort")
                .map { it.trim().toInt() }
                .orElse(8820),
        )

        val outputDirProvider =
            extension.outputDir.map { path -> project.layout.projectDirectory.dir(path) }

        val crawl =
            project.tasks.register("crawlDashboard", CrawlDashboardTask::class.java) { task ->
                task.group = DashboardConstants.DASHBOARD_GROUP
                task.description = "Crawls the workspace governance files and writes the JSON pivot."
                task.rootDir.set(extension.configPath.map { path -> project.layout.projectDirectory.dir(path) })
                task.projectDir.set(project.layout.projectDirectory)
                task.workspaceBacklogPath.set(extension.workspaceBacklog)
                task.criticalPath.set(extension.criticalPath)
                task.dataFile.set(
                    extension.dataDir.map { path ->
                        project.layout.projectDirectory
                            .dir(path)
                            .file("dashboard-data.json")
                    },
                )
                task.governanceFiles.from(
                    project.provider {
                        val root =
                            project.projectDir
                                .toPath()
                                .resolve(extension.configPath.get())
                                .toFile()
                        val files = mutableListOf<Any>()
                        files.add(
                            project.fileTree(root).apply {
                                include("**/INDEX.adoc", "**/SESSIONS_HISTORY.adoc", "**/BACKLOG.adoc")
                                exclude(
                                    "**/build/**",
                                    "**/.gradle/**",
                                    "**/.git/**",
                                    "**/.kotlin/**",
                                    "**/.idea/**",
                                    "**/node_modules/**",
                                    "**/out/**",
                                    "**/target/**",
                                    "**/dist/**",
                                    "**/.venv/**",
                                    "**/venv/**",
                                    "**/__pycache__/**",
                                    "**/backup/**",
                                    "**/archives/**",
                                )
                            },
                        )
                        val backlog =
                            extension.workspaceBacklog
                                .get()
                                .takeIf { it.isNotBlank() }
                                ?.let {
                                    project.projectDir
                                        .toPath()
                                        .resolve(it)
                                        .toFile()
                                }
                        if (backlog != null && backlog.isFile) files.add(backlog)
                        files
                    },
                )
            }

        project.tasks.register("generateDashboard", GenerateDashboardTask::class.java) { task ->
            task.group = DashboardConstants.DASHBOARD_GROUP
            task.description = "Generates the static Panorama and borough sheets from the crawled data."
            task.dependsOn(crawl)
            task.dataFile.set(crawl.flatMap { it.dataFile })
            task.outputDir.set(outputDirProvider)
        }

        project.tasks.register("publishDashboardSite", PublishDashboardTask::class.java) { task ->
            task.dependsOn("generateDashboard")
            task.outputDir.set(outputDirProvider)
            task.publishDir.set(
                extension.publishDir.map { path -> project.layout.projectDirectory.dir(path) },
            )
        }

        project.tasks.register("serve", ServeDashboardTask::class.java) { task ->
            task.group = DashboardConstants.DASHBOARD_GROUP
            task.description =
                "Serves the generated dashboard locally (default http://localhost:8820, Ctrl+C to stop)."
            task.dependsOn("generateDashboard")
            task.outputDir.set(outputDirProvider)
            task.host.set(extension.serveHost)
            task.port.set(extension.servePort)
        }
    }
}
