package education.cccp.dashboard

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

    @get:Input
    @get:Optional
    abstract val boroughs: ListProperty<String>

    @get:Internal
    abstract val foundryDir: DirectoryProperty

    init {
        configPath.convention("foundry")
        outputDir.convention("build/dashboard")
        boroughs.convention(emptyList())
    }
}
