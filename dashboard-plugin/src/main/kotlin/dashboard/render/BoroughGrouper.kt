package dashboard.render

import dashboard.model.BoroughData
import dashboard.model.BoroughStatus
import dashboard.model.DashboardData
import dashboard.model.EpicData

/**
 * Groups EPICs by borough for dashboard rendering.
 *
 * Grouping rules:
 * - one group per known borough, sorted by DAG level
 * - EPICs inside a borough group are sorted by priority then id
 * - EPICs without a borough are grouped under "UNASSIGNED"
 */
object BoroughGrouper {

    fun groupByBorough(data: DashboardData): List<BoroughGroup> {
        val epicsByBorough = data.epics.groupBy { it.borough }
        val groups = data.boroughs.sortedBy { it.dagLevel }.map { borough ->
            BoroughGroup(
                name = borough.name,
                dagLevel = borough.dagLevel,
                project = borough.project,
                status = borough.status.name,
                epics = sortEpics(epicsByBorough[borough.name] ?: emptyList())
            )
        }.toMutableList()

        epicsByBorough[""]?.let { orphans ->
            groups += BoroughGroup(
                name = "UNASSIGNED",
                dagLevel = "N?",
                project = "",
                status = BoroughStatus.ACTIVE.name,
                epics = sortEpics(orphans)
            )
        }

        return groups
    }

    private fun sortEpics(epics: List<EpicData>): List<EpicData> =
        epics.sortedWith(compareBy({ it.priority }, { it.id }))
}
