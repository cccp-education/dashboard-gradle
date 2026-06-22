package dashboard.timeline

import dashboard.model.DashboardData
import dashboard.model.EpicStatus

/**
 * Domain service that builds a [Timeline] from [DashboardData].
 *
 * Each EPIC becomes a milestone. The milestone date is resolved from the
 * linked session number when available; otherwise a far-future placeholder
 * date is used for planned work.
 */
object TimelineBuilder {

    const val DEFAULT_PLANNED_DATE = "2099-12-31"

    fun build(data: DashboardData): Timeline {
        val dagLevelByBorough = data.boroughs.associate { it.name to it.dagLevel }
        val sessionDateByNumber = data.sessions.associate { it.number.padStart(3, '0') to it.date }

        val milestones = data.epics.mapNotNull { epic ->
            val borough = epic.borough.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val dagLevel = dagLevelByBorough[borough] ?: ""
            val sessionDate = epic.session.takeIf { it.isNotBlank() }
                ?.let { sessionDateByNumber[it.padStart(3, '0')] }
                ?: DEFAULT_PLANNED_DATE

            val (status, date) = when (epic.status) {
                EpicStatus.TERMINE -> MilestoneStatus.DONE to sessionDate
                EpicStatus.EN_COURS -> MilestoneStatus.IN_PROGRESS to sessionDate
                EpicStatus.BLOQUE,
                EpicStatus.PLANIFIE -> MilestoneStatus.PLANNED to DEFAULT_PLANNED_DATE
            }

            Milestone(
                id = epic.id,
                label = epic.title,
                date = date,
                borough = borough,
                dagLevel = dagLevel,
                type = MilestoneType.EPIC_RELEASE,
                status = status
            )
        }

        return Timeline(milestones)
    }
}
