package dashboard.serve

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.nio.file.Files
import java.util.concurrent.CountDownLatch

/**
 * `serve` (DSH-9.1) — serves the generated dashboard locally.
 *
 * Runs after `generateDashboard` and blocks until interrupted (Ctrl+C).
 * Compatible with Gradle continuous build (`./gradlew -t serve`).
 */
@DisableCachingByDefault(because = "Long-running local development server")
abstract class ServeDashboardTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val host: Property<String>

    @get:Input
    abstract val port: Property<Int>

    @TaskAction
    fun serve() {
        val root = outputDir.get().asFile.toPath()
        require(Files.isDirectory(root)) {
            "Dashboard output directory not found: $root — run generateDashboard first."
        }
        val hostValue = host.get()
        val portValue = port.get()
        require(portValue in 1..65535) {
            "Invalid dashboard serve port: $portValue (expected 1..65535)."
        }

        val server = DashboardServer(root, hostValue, portValue).start()
        logger.lifecycle("Serving $root on http://$hostValue:${server.boundPort} (Ctrl+C to stop)")
        try {
            CountDownLatch(1).await()
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        } finally {
            server.stop()
            logger.lifecycle("Dashboard server stopped")
        }
    }
}
