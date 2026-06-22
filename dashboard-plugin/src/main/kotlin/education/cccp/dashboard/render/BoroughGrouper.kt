package education.cccp.dashboard.render

import education.cccp.dashboard.model.BoroughData
import education.cccp.dashboard.model.BoroughStatus
import education.cccp.dashboard.model.DashboardData
import education.cccp.dashboard.model.EpicData

/**
 * Groupe les EPICs par borough pour le rendu du dashboard.
 *
 * Règles de groupement :
 * - un groupe par borough connu, trié par niveau DAG
 * - les EPICs d'un borough sont triés par priorité puis identifiant
 * - les EPICs sans borough sont regroupés dans un groupe "UNASSIGNED"
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
