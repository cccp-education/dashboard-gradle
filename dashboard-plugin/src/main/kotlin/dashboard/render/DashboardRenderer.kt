package dashboard.render

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dashboard.model.DashboardData
import dashboard.model.EpicStatus
import dashboard.timeline.TimelineBuilder
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import java.nio.file.Files
import java.nio.file.Path

/**
 * Renders the static dashboard from [DashboardData].
 *
 * Thymeleaf is initialized with a [ClassLoaderTemplateResolver] pointing to the
 * embedded templates in `src/main/resources/templates/`. The output produces:
 * - `index.html`: main page with EPIC matrix, activity stream, and DAG graph
 * - `styles.css`: stylesheet with the status palette
 */
class DashboardRenderer {

    private val templateEngine: TemplateEngine = createTemplateEngine()

    private val mapper = jacksonObjectMapper().apply {
        enable(SerializationFeature.INDENT_OUTPUT)
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

    fun render(data: DashboardData, outputDir: Path) {
        Files.createDirectories(outputDir)

        val dagGraph = DagGraphBuilder.build(data)

        val page = DashboardPage(
            title = "Dashboard Workspace CCCP",
            boroughs = data.boroughs.sortedBy { it.dagLevel },
            epics = data.epics.sortedWith(compareBy({ it.priority }, { it.id })),
            sessions = data.sessions.sortedByDescending { it.number.padStart(3, '0') },
            stats = computeStats(data),
            boroughGroups = BoroughGrouper.groupByBorough(data),
            dagGraph = dagGraph,
            dagGraphJson = mapper.writeValueAsString(dagGraph),
            timeline = TimelineBuilder.build(data)
        )

        val context = Context().apply {
            setVariable("page", page)
            setVariable("statusHelper", StatusCssHelper)
        }

        val html = templateEngine.process("dashboard", context)
        Files.writeString(outputDir.resolve("index.html"), html)

        copyStaticAsset("static/styles.css", outputDir.resolve("styles.css"))
    }

    /**
     * Loads JSON data produced by the `crawlDashboard` task and generates the site.
     */
    fun renderFromJson(jsonPath: Path, outputDir: Path) {
        val data: DashboardData = mapper.readValue(jsonPath.toFile())
        render(data, outputDir)
    }

    private fun computeStats(data: DashboardData): DashboardStats {
        val byStatus = data.epics.groupingBy { it.status }.eachCount()
        return DashboardStats(
            boroughCount = data.boroughs.size,
            epicCount = data.epics.size,
            doneCount = byStatus[EpicStatus.TERMINE] ?: 0,
            inProgressCount = byStatus[EpicStatus.EN_COURS] ?: 0,
            plannedCount = byStatus[EpicStatus.PLANIFIE] ?: 0,
            blockedCount = byStatus[EpicStatus.BLOQUE] ?: 0
        )
    }

    private fun copyStaticAsset(resourcePath: String, target: Path) {
        javaClass.classLoader.getResourceAsStream(resourcePath)?.use { input ->
            Files.copy(input, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        } ?: throw IllegalStateException("Static asset not found: $resourcePath")
    }

    private fun createTemplateEngine(): TemplateEngine {
        val resolver = ClassLoaderTemplateResolver().apply {
            prefix = "templates/"
            suffix = ".thyme"
            templateMode = TemplateMode.HTML
            isCacheable = false
        }
        return TemplateEngine().apply {
            setTemplateResolver(resolver)
        }
    }

    companion object {
        /**
         * Returns the CSS class associated with an EPIC status.
         */
        fun statusCssClass(status: EpicStatus): String = StatusCssHelper.statusCssClass(status)
    }
}

/**
 * Instantiabable helper exposed to the Thymeleaf template.
 */
object StatusCssHelper {
    fun statusCssClass(status: EpicStatus): String = when (status) {
        EpicStatus.TERMINE -> "status-done"
        EpicStatus.EN_COURS -> "status-in-progress"
        EpicStatus.PLANIFIE -> "status-planned"
        EpicStatus.BLOQUE -> "status-blocked"
    }
}
