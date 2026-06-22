package education.cccp.dashboard.render

import education.cccp.dashboard.model.BoroughData
import education.cccp.dashboard.model.EpicData
import education.cccp.dashboard.model.EpicStatus
import education.cccp.dashboard.model.SessionActivity

/**
 * Modèle de page pour le rendu Thymeleaf du dashboard.
 *
 * Ce data class est le DTO exposé au template. Il structure les données brutes
 * du modèle [education.cccp.dashboard.model.DashboardData] en une forme adaptée
 * à la présentation (matrix EPIC, activity stream, statistiques).
 */
data class DashboardPage(
    val title: String,
    val boroughs: List<BoroughData>,
    val epics: List<EpicData>,
    val sessions: List<SessionActivity>,
    val stats: DashboardStats
)

data class DashboardStats(
    val boroughCount: Int,
    val epicCount: Int,
    val doneCount: Int,
    val inProgressCount: Int,
    val plannedCount: Int,
    val blockedCount: Int
)
