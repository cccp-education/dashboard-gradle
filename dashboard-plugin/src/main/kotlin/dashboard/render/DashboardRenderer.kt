package dashboard.render

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dashboard.model.DashboardData
import dashboard.model.EpicStatus
import dashboard.model.SessionActivity
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
 * - `index.html`: main page with EPIC matrix, activity stream, DAG layers and timeline
 * - `styles.css`: stylesheet with the status palette
 * - `vendor/bootstrap.min.css`: vendored Bootstrap (offline render, no CDN)
 */
class DashboardRenderer {
    private val templateEngine: TemplateEngine = createTemplateEngine()

    private val mapper =
        jacksonObjectMapper().apply {
            enable(SerializationFeature.INDENT_OUTPUT)
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        }

    fun render(
        data: DashboardData,
        outputDir: Path,
    ) {
        Files.createDirectories(outputDir)

        val dagGraph = DagGraphBuilder.build(data)

        val page =
            DashboardPage(
                title = "Dashboard Workspace CCCP",
                boroughs = data.boroughs.sortedBy { it.dagLevel },
                epics = data.epics.sortedWith(compareBy({ it.priority }, { it.id })),
                sessions = recentSessions(data.sessions),
                stats = computeStats(data),
                boroughGroups = BoroughGrouper.groupByBorough(data),
                dagLevels = groupDagLevels(dagGraph),
                timeline = TimelineBuilder.build(data),
                sessionCount = data.sessions.size,
            )

        val context =
            Context().apply {
                setVariable("page", page)
                setVariable("statusHelper", StatusCssHelper)
            }

        val html = templateEngine.process("dashboard", context)

        // ── Flat site (legacy, consumed by direct preview)
        Files.writeString(outputDir.resolve("index.html"), html)
        copyStaticAsset("static/styles.css", outputDir.resolve("styles.css"))
        copyStaticAsset(VENDOR_BOOTSTRAP, outputDir.resolve("vendor/bootstrap.min.css"))

        // ── JBake-compatible site structure, consumable by bakery-gradle serve/bake
        val jbakeDir = outputDir.resolve("jbake")
        Files.createDirectories(jbakeDir)
        writeJbakeProperties(jbakeDir.resolve("jbake.properties"))
        Files.createDirectories(jbakeDir.resolve("content"))
        val jbakeHtml =
            html
                .replace("href=\"styles.css\"", "href=\"css/styles.css\"")
                .replace("href=\"vendor/bootstrap.min.css\"", "href=\"css/bootstrap.min.css\"")
        Files.writeString(jbakeDir.resolve("content/index.html"), wrapAsJbakeContent(jbakeHtml))
        Files.createDirectories(jbakeDir.resolve("templates"))
        copyStaticAsset("templates/jbake-index.thyme", jbakeDir.resolve("templates/index.thyme"))
        Files.createDirectories(jbakeDir.resolve("assets/css"))
        copyStaticAsset("static/styles.css", jbakeDir.resolve("assets/css/styles.css"))
        copyStaticAsset(VENDOR_BOOTSTRAP, jbakeDir.resolve("assets/css/bootstrap.min.css"))
        Files.createDirectories(jbakeDir.resolve("assets/js"))
        Files.writeString(jbakeDir.resolve("assets/js/dashboard-data.json"), mapper.writeValueAsString(data))
    }

    /**
     * Loads JSON data produced by the `crawlDashboard` task and generates the site.
     */
    fun renderFromJson(
        jsonPath: Path,
        outputDir: Path,
    ) {
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
            blockedCount = byStatus[EpicStatus.BLOQUE] ?: 0,
        )
    }

    /**
     * Keeps the activity stream readable when the workspace holds thousands of
     * sessions: the full set stays in `dashboard-data.json`, the page shows the
     * most recent entries only.
     */
    private fun recentSessions(sessions: List<SessionActivity>): List<SessionActivity> =
        sessions
            .sortedWith(compareByDescending<SessionActivity> { it.date }.thenByDescending { it.number })
            .take(RECENT_SESSIONS_LIMIT)

    private fun groupDagLevels(graph: DagGraph): List<DagLevelGroup> =
        graph.nodes
            .groupBy { it.group.ifBlank { UNKNOWN_DAG_LEVEL } }
            .toList()
            .sortedWith(compareBy({ DagGraphBuilder.parseDagLevel(it.first) }, { it.first }))
            .map { (level, nodes) ->
                DagLevelGroup(level = level, nodes = nodes.sortedBy { it.project })
            }

    private fun copyStaticAsset(
        resourcePath: String,
        target: Path,
    ) {
        target.parent?.let { Files.createDirectories(it) }
        javaClass.classLoader.getResourceAsStream(resourcePath)?.use { input ->
            Files.copy(input, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        } ?: throw IllegalStateException("Static asset not found: $resourcePath")
    }

    private fun writeJbakeProperties(target: Path) {
        val props =
            listOf(
                "site.host=https://cccp.education/dashboard/",
                "render.index=false",
                "template.page.file=index.thyme",
                "render.archive=false",
                "render.tags=false",
                "render.sitemap=false",
                "render.feed=false",
            )
        Files.writeString(target, props.joinToString("\n", postfix = "\n"))
    }

    private fun wrapAsJbakeContent(html: String): String =
        """title=Dashboard Workspace CCCP
type=page
status=published
date=2026-06-23
~~~~~~
$html
""".trimEnd()

    private fun createTemplateEngine(): TemplateEngine {
        val resolver =
            ClassLoaderTemplateResolver().apply {
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
        private const val VENDOR_BOOTSTRAP = "static/vendor/bootstrap.min.css"
        private const val RECENT_SESSIONS_LIMIT = 60
        private const val UNKNOWN_DAG_LEVEL = "N?"

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
    fun statusCssClass(status: EpicStatus): String =
        when (status) {
            EpicStatus.TERMINE -> "status-done"
            EpicStatus.EN_COURS -> "status-in-progress"
            EpicStatus.PLANIFIE -> "status-planned"
            EpicStatus.BLOQUE -> "status-blocked"
        }
}
