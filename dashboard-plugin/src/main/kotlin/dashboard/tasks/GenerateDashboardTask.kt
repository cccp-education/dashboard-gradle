package dashboard.tasks

import dashboard.render.DashboardRenderer
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Renders the static dashboard site (Panorama + borough sheets) from the JSON
 * pivot produced by [CrawlDashboardTask].
 *
 * The pivot lives outside [outputDir], so the two tasks have disjoint outputs
 * and both can be cacheable.
 */
@CacheableTask
abstract class GenerateDashboardTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val dataFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val target = outputDir.get().asFile.toPath()
        DashboardRenderer().renderFromJson(dataFile.get().asFile.toPath(), target)
        logger.lifecycle("Dashboard site generated: ${target.resolve("index.html")}")
    }
}
