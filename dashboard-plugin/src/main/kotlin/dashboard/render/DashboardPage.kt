package dashboard.render

import dashboard.model.BoroughData
import dashboard.model.EpicData
import dashboard.model.SessionActivity

/**
 * Thymeleaf page model exposed to the dashboard template.
 *
 * This DTO structures raw [dashboard.model.DashboardData] into a presentation-friendly
 * shape (EPIC matrix, activity stream, statistics, DAG graph).
 */
data class DashboardPage(
    val title: String,
    val boroughs: List<BoroughData>,
    val epics: List<EpicData>,
    val sessions: List<SessionActivity>,
    val stats: DashboardStats,
    val boroughGroups: List<BoroughGroup>,
    val dagGraph: DagGraph,
    val dagGraphJson: String
)

data class DashboardStats(
    val boroughCount: Int,
    val epicCount: Int,
    val doneCount: Int,
    val inProgressCount: Int,
    val plannedCount: Int,
    val blockedCount: Int
)

data class BoroughGroup(
    val name: String,
    val dagLevel: String,
    val project: String,
    val status: String,
    val epics: List<EpicData>
)
