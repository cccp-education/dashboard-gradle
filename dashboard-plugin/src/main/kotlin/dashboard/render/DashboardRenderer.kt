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
import kotlin.math.roundToInt

/**
 * Renders the static dashboard from [DashboardData].
 *
 * Produces the Panorama homepage (`index.html`) and one static borough sheet
 * per borough (`borough-<slug>.html`), plus the offline assets and the
 * JBake-compatible export.
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

        val panorama = buildPanorama(data)
        val panoramaContext = contextOf(panorama)
        val panoramaHtml = templateEngine.process("dashboard", panoramaContext)

        Files.writeString(outputDir.resolve("index.html"), panoramaHtml)
        copyStaticAsset("static/styles.css", outputDir.resolve("styles.css"))
        copyStaticAsset(VENDOR_BOOTSTRAP, outputDir.resolve("vendor/bootstrap.min.css"))

        val boroughPages = renderBoroughPages(data, outputDir)
        writeJbakeSite(data, outputDir, panoramaHtml, boroughPages)

        mapper.writeValue(outputDir.resolve("dashboard-data.json").toFile(), data)
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

    private fun contextOf(page: Any): Context =
        Context().apply {
            setVariable("page", page)
            setVariable("statusHelper", StatusCssHelper)
        }

    private fun buildPanorama(data: DashboardData): DashboardPage {
        val timeline = TimelineBuilder.build(data)
        val progressByBorough = data.progress.byBorough.associateBy { it.borough }
        val bars =
            data.boroughs
                .map { borough ->
                    val progress = progressByBorough[borough.name]
                    BoroughProgressView(
                        name = borough.name,
                        project = borough.project,
                        dagLevel = borough.dagLevel.ifBlank { "N?" },
                        slugFile = Slug.fileName(borough.name),
                        percent = progress?.percent ?: 0,
                        donePoints = progress?.donePoints ?: 0,
                        totalPoints = progress?.totalPoints ?: 0,
                        doneEpics = progress?.doneEpics ?: 0,
                        totalEpics = progress?.totalEpics ?: 0,
                    )
                }.sortedWith(compareByDescending<BoroughProgressView> { it.percent }.thenBy { it.name })

        return DashboardPage(
            title = "Dashboard Workspace CCCP",
            stats = computeStats(data),
            progress = data.progress,
            sessionCount = data.sessions.size,
            rings = ringViews(data),
            milestones = timeline.milestones,
            boroughBars = bars,
            dagLevels = structureLevels(data),
        )
    }

    private fun ringViews(data: DashboardData): List<CriticalPathRingView> =
        data.criticalPath.map { ring ->
            val matched =
                ring.projects.mapNotNull { project ->
                    data.progress.byBorough.firstOrNull { it.project.equals(project, ignoreCase = true) }
                }
            val done = matched.sumOf { it.donePoints }
            val total = matched.sumOf { it.totalPoints }
            val percent = if (total == 0) 0 else (done * 100.0 / total).roundToInt().coerceIn(0, 100)
            val state =
                ring.state.ifBlank {
                    when {
                        total == 0 -> "planned"
                        percent >= 100 -> "done"
                        percent > 0 -> "in-progress"
                        else -> "planned"
                    }
                }
            CriticalPathRingView(
                ring = ring.ring,
                label = ring.label,
                state = state,
                percent = percent,
                donePoints = done,
                totalPoints = total,
                projects = ring.projects.joinToString(", "),
            )
        }

    private fun structureLevels(data: DashboardData): List<StructureLevel> {
        val progressByBorough = data.progress.byBorough.associateBy { it.borough }
        val nodes = DagGraphBuilder.build(data).nodes
        return nodes
            .groupBy { it.group.ifBlank { "N?" } }
            .toList()
            .sortedWith(compareBy({ DagGraphBuilder.parseDagLevel(it.first) }, { it.first }))
            .map { (level, levelNodes) ->
                StructureLevel(
                    level = level,
                    nodes =
                        levelNodes.sortedBy { it.project }.map { node ->
                            StructureNode(
                                name = node.id,
                                project = node.project,
                                slugFile = Slug.fileName(node.id),
                                percent = progressByBorough[node.id]?.percent ?: 0,
                            )
                        },
                )
            }
    }

    private fun renderBoroughPages(
        data: DashboardData,
        outputDir: Path,
    ): List<Pair<String, String>> {
        val progressByBorough = data.progress.byBorough.associateBy { it.borough }
        return data.boroughs.map { borough ->
            val progress = progressByBorough[borough.name]
            val page =
                BoroughPage(
                    title = "${borough.name} — Dashboard CCCP",
                    name = borough.name,
                    project = borough.project,
                    dagLevel = borough.dagLevel.ifBlank { "N?" },
                    role = borough.role,
                    status = borough.status.name,
                    percent = progress?.percent ?: 0,
                    donePoints = progress?.donePoints ?: 0,
                    totalPoints = progress?.totalPoints ?: 0,
                    doneEpics = progress?.doneEpics ?: 0,
                    totalEpics = progress?.totalEpics ?: 0,
                    epics =
                        data.epics
                            .filter { it.borough == borough.name }
                            .sortedWith(compareBy({ it.priority }, { it.id })),
                    sessions = recentSessions(data.sessions, borough.name),
                )
            val fileName = Slug.fileName(borough.name)
            val html = templateEngine.process("borough", contextOf(page))
            Files.writeString(outputDir.resolve(fileName), html)
            fileName to html
        }
    }

    private fun writeJbakeSite(
        data: DashboardData,
        outputDir: Path,
        panoramaHtml: String,
        boroughPages: List<Pair<String, String>>,
    ) {
        val jbakeDir = outputDir.resolve("jbake")
        Files.createDirectories(jbakeDir)
        writeJbakeProperties(jbakeDir.resolve("jbake.properties"))
        Files.createDirectories(jbakeDir.resolve("content"))
        Files.writeString(jbakeDir.resolve("content/index.html"), wrapAsJbakeContent(toJbakeAssets(panoramaHtml)))
        for ((fileName, html) in boroughPages) {
            Files.writeString(jbakeDir.resolve("content/$fileName"), wrapAsJbakeContent(toJbakeAssets(html)))
        }
        Files.createDirectories(jbakeDir.resolve("templates"))
        copyStaticAsset("templates/jbake-index.thyme", jbakeDir.resolve("templates/index.thyme"))
        Files.createDirectories(jbakeDir.resolve("assets/css"))
        copyStaticAsset("static/styles.css", jbakeDir.resolve("assets/css/styles.css"))
        copyStaticAsset(VENDOR_BOOTSTRAP, jbakeDir.resolve("assets/css/bootstrap.min.css"))
        Files.createDirectories(jbakeDir.resolve("assets/js"))
        Files.writeString(jbakeDir.resolve("assets/js/dashboard-data.json"), mapper.writeValueAsString(data))
    }

    private fun toJbakeAssets(html: String): String =
        html
            .replace("href=\"vendor/bootstrap.min.css\"", "href=\"css/bootstrap.min.css\"")
            .replace("href=\"styles.css\"", "href=\"css/styles.css\"")

    private fun recentSessions(
        sessions: List<SessionActivity>,
        borough: String,
    ): List<SessionActivity> =
        sessions
            .filter { it.borough == borough }
            .sortedWith(compareByDescending<SessionActivity> { it.date }.thenByDescending { it.number })
            .take(RECENT_SESSIONS_LIMIT)

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
        private const val RECENT_SESSIONS_LIMIT = 40

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
