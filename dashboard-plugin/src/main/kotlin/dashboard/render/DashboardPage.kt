package dashboard.render

import dashboard.model.EpicData
import dashboard.model.ProgressSummary
import dashboard.model.SessionActivity
import dashboard.timeline.Milestone

/**
 * Thymeleaf model for the Panorama homepage (3 zones: vision / progress /
 * structure).
 */
data class DashboardPage(
    val title: String,
    val stats: DashboardStats,
    val progress: ProgressSummary,
    val sessionCount: Int,
    val rings: List<CriticalPathRingView>,
    val milestones: List<Milestone>,
    val boroughBars: List<BoroughProgressView>,
    val dagLevels: List<StructureLevel>,
)

data class DashboardStats(
    val boroughCount: Int,
    val epicCount: Int,
    val doneCount: Int,
    val inProgressCount: Int,
    val plannedCount: Int,
    val blockedCount: Int,
)

data class CriticalPathRingView(
    val ring: String,
    val label: String,
    val state: String,
    val percent: Int,
    val donePoints: Int,
    val totalPoints: Int,
    val projects: String,
)

data class BoroughProgressView(
    val name: String,
    val project: String,
    val dagLevel: String,
    val slugFile: String,
    val percent: Int,
    val donePoints: Int,
    val totalPoints: Int,
    val doneEpics: Int,
    val totalEpics: Int,
) {
    val hasPoints: Boolean get() = totalPoints > 0
}

data class StructureLevel(
    val level: String,
    val nodes: List<StructureNode>,
)

data class StructureNode(
    val name: String,
    val project: String,
    val slugFile: String,
    val percent: Int,
)

/**
 * Thymeleaf model for a borough sheet (level 1 page).
 */
data class BoroughPage(
    val title: String,
    val name: String,
    val project: String,
    val dagLevel: String,
    val role: String,
    val status: String,
    val percent: Int,
    val donePoints: Int,
    val totalPoints: Int,
    val doneEpics: Int,
    val totalEpics: Int,
    val epics: List<EpicData>,
    val sessions: List<SessionActivity>,
) {
    val hasPoints: Boolean get() = totalPoints > 0
}

/** Grouping used by the historical EPIC matrix (kept for the BDD suite). */
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
