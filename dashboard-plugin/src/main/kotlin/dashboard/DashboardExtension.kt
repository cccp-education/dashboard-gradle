package dashboard

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional

abstract class DashboardExtension {
    @get:Input
    abstract val configPath: Property<String>

    @get:Input
    abstract val outputDir: Property<String>

    /** Where `crawlDashboard` writes the JSON pivot (outside the site dir). */
    @get:Input
    abstract val dataDir: Property<String>

    @get:Input
    @get:Optional
    abstract val publishDir: Property<String>

    @get:Input
    @get:Optional
    abstract val boroughs: ListProperty<String>

    /** Optional workspace-root `BACKLOG.adoc` (GBL transverse EPICs). */
    @get:Input
    @get:Optional
    abstract val workspaceBacklog: Property<String>

    /** MVP0 critical path rings (A1 → A4), rendered in the Panorama vision zone. */
    @get:Input
    @get:Optional
    abstract val criticalPath: ListProperty<CriticalPathRing>

    @get:Input
    abstract val serveHost: Property<String>

    @get:Input
    abstract val servePort: Property<Int>

    @get:Internal
    abstract val foundryDir: DirectoryProperty

    init {
        configPath.convention("foundry")
        outputDir.convention("build/dashboard")
        dataDir.convention("build/dashboard-data")
        publishDir.convention("build/dashboard-publish")
        boroughs.convention(emptyList())
        workspaceBacklog.convention("")
        criticalPath.convention(emptyList())
    }
}
