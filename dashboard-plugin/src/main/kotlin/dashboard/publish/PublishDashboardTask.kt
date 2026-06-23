package dashboard.publish

import dashboard.DashboardConstants
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files

/**
 * Publishes the generated dashboard site to a directory that can be consumed
 * by downstream deploy tasks (e.g. runner-gradle / bakery-gradle gh-pages pipeline).
 *
 * The task copies the JBake-compatible site structure (outputDir/jbake) into the
 * configured publish directory. This lets bakery-gradle consume the dashboard as a
 * regular JBake site via `bake.srcPath`.
 *
 * The task declares its inputs and outputs so Gradle can track up-to-date checks
 * and so N3 runners can depend on the published directory.
 */
@CacheableTask
abstract class PublishDashboardTask : DefaultTask() {

    init {
        group = DashboardConstants.DASHBOARD_GROUP
        description = "Publishes the generated dashboard site to the publish directory."
    }

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val outputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val publishDir: DirectoryProperty

    @TaskAction
    fun publish() {
        val output = outputDir.get().asFile.toPath()
        val publish = publishDir.get().asFile.toPath()
        val jbakeOutput = output.resolve("jbake")

        if (!Files.exists(jbakeOutput)) {
            logger.warn("Dashboard JBake output not found: $jbakeOutput. Run generateDashboard first.")
            return
        }

        logger.lifecycle("Publishing dashboard JBake site from $jbakeOutput to $publish")
        Files.createDirectories(publish)
        jbakeOutput.toFile().copyRecursively(publish.toFile(), overwrite = true)
        logger.lifecycle("Dashboard published: ${publish.resolve("jbake.properties")}")
    }
}
