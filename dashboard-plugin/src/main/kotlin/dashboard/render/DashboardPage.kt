package dashboard.render

import dashboard.model.BoroughData
import dashboard.model.EpicData
import dashboard.model.ProgressSummary
import dashboard.model.SessionActivity
import dashboard.timeline.Timeline

/**
 * Thymeleaf page model exposed to the dashboard template.
 *
 * This DTO structures raw [dashboard.model.DashboardData] into a presentation-friendly
 * shape (EPIC matrix, activity stream, statistics, DAG layers, timeline).
 */
data class DashboardPage(
    val title: String,
    val boroughs: List<BoroughData>,
    val epics: List<EpicData>,
    val sessions: List<SessionActivity>,
    val stats: DashboardStats,
    val progress: ProgressSummary,
    val boroughGroups: List<BoroughGroup>,
    val dagLevels: List<DagLevelGroup>,
    val timeline: Timeline,
    val sessionCount: Int,
) {
    fun progressLabel(borough: String): String {
        val entry = progress.byBorough.firstOrNull { it.borough == borough } ?: return "—"
        return if (entry.totalPoints == 0) "—" else "${entry.percent} %"
    }
}

data class DashboardStats(
    val boroughCount: Int,
    val epicCount: Int,
    val doneCount: Int,
    val inProgressCount: Int,
    val plannedCount: Int,
    val blockedCount: Int,
)

data class BoroughGroup(
    val name: String,
    val dagLevel: String,
    val project: String,
    val status: String,
    val epics: List<EpicData>,
    val donePoints: Int,
    val totalPoints: Int,
    val percent: Int,
)

data class DagLevelGroup(
    val level: String,
    val nodes: List<DagNodeView>,
)
