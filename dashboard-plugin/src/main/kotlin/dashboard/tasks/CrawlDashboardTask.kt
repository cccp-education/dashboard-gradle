package dashboard.tasks

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dashboard.CriticalPathRing
import dashboard.IndexCrawler
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files

/**
 * Crawls the governance files of the workspace and writes the `dashboard-data.json`
 * pivot.
 *
 * [governanceFiles] is a filtered file collection (`INDEX.adoc`,
 * `SESSIONS_HISTORY.adoc` and the optional workspace `BACKLOG.adoc`) so build
 * outputs, caches and backups never invalidate the task.
 */
@CacheableTask
abstract class CrawlDashboardTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val governanceFiles: ConfigurableFileCollection

    @get:Internal
    abstract val rootDir: DirectoryProperty

    @get:Internal
    abstract val projectDir: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val workspaceBacklogPath: Property<String>

    @get:Input
    @get:Optional
    abstract val criticalPath: ListProperty<CriticalPathRing>

    @get:OutputFile
    abstract val dataFile: RegularFileProperty

    @TaskAction
    fun crawl() {
        val root = rootDir.get().asFile.toPath()
        val projectRoot = projectDir.get().asFile.toPath()
        val backlog =
            workspaceBacklogPath
                .orNull
                ?.takeIf { it.isNotBlank() }
                ?.let { projectRoot.resolve(it) }
                ?.takeIf { Files.isRegularFile(it) }
        val startedAt = System.nanoTime()
        val data =
            IndexCrawler()
                .crawlDirectory(root, backlog)
                .copy(criticalPath = criticalPath.getOrElse(emptyList()))
        val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000

        val output = dataFile.get().asFile
        output.parentFile?.mkdirs()
        val mapper =
            jacksonObjectMapper().apply {
                enable(SerializationFeature.INDENT_OUTPUT)
            }
        mapper.writeValue(output, data)
        logger.lifecycle(
            "Dashboard data written to $output (${data.boroughs.size} boroughs, ${data.epics.size} epics, " +
                "${data.sessions.size} sessions, ${data.progress.percent}% / " +
                "${data.progress.donePoints}/${data.progress.totalPoints} pts) in ${elapsedMs}ms",
        )
    }
}
