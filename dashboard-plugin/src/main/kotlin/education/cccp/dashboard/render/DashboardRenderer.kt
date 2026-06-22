package education.cccp.dashboard.render

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import education.cccp.dashboard.model.DashboardData
import education.cccp.dashboard.model.EpicStatus
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import java.nio.file.Files
import java.nio.file.Path

/**
 * Rend le dashboard statique à partir de [DashboardData].
 *
 * Le moteur Thymeleaf est initialisé avec un [ClassLoaderTemplateResolver] pointant
 * sur les templates embarqués dans `src/main/resources/templates/`. Le rendu produit :
 * - `index.html` : page principale avec la matrice EPIC et l'activity stream
 * - `styles.css` : feuille de style avec la palette de statuts
 */
class DashboardRenderer {

    private val templateEngine: TemplateEngine = createTemplateEngine()

    private val mapper = jacksonObjectMapper().apply {
        enable(SerializationFeature.INDENT_OUTPUT)
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

    fun render(data: DashboardData, outputDir: Path) {
        Files.createDirectories(outputDir)

        val page = DashboardPage(
            title = "Dashboard Workspace CCCP",
            boroughs = data.boroughs.sortedBy { it.dagLevel },
            epics = data.epics.sortedWith(compareBy({ it.priority }, { it.id })),
            sessions = data.sessions.sortedByDescending { it.number.padStart(3, '0') },
            stats = computeStats(data)
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
     * Charge les données JSON produites par la tâche `crawlDashboard` et génère le site.
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
         * Renvoie la classe CSS associée à un statut d'EPIC.
         */
        fun statusCssClass(status: EpicStatus): String = StatusCssHelper.statusCssClass(status)
    }
}

/**
 * Helper instanciable exposé au template Thymeleaf.
 */
object StatusCssHelper {
    fun statusCssClass(status: EpicStatus): String = when (status) {
        EpicStatus.TERMINE -> "status-done"
        EpicStatus.EN_COURS -> "status-in-progress"
        EpicStatus.PLANIFIE -> "status-planned"
        EpicStatus.BLOQUE -> "status-blocked"
    }
}
