package dashboard.render

import dashboard.model.DashboardData

/**
 * Builds a level-oriented DAG graph from dashboard data.
 *
 * Rules:
 * - one node per borough (or DagNode fallback)
 * - level is extracted from the dagLevel field ("N0" → 0, "N?" → 99)
 * - every node at level k is connected to every node at level k+1
 */
object DagGraphBuilder {

    fun build(data: DashboardData): DagGraph {
        val nodes = extractNodes(data)
        if (nodes.isEmpty()) return DagGraph(emptyList(), emptyList())

        val byLevel = nodes.groupBy { it.level }.toSortedMap()
        val edges = mutableListOf<DagEdgeView>()
        val levels = byLevel.keys.toList()
        for (i in 0 until levels.size - 1) {
            val current = byLevel[levels[i]] ?: emptyList()
            val next = byLevel[levels[i + 1]] ?: emptyList()
            for (source in current) {
                for (target in next) {
                    edges += DagEdgeView(from = source.id, to = target.id)
                }
            }
        }
        return DagGraph(nodes, edges)
    }

    private fun extractNodes(data: DashboardData): List<DagNodeView> {
        val boroughNodes = data.boroughs.map { borough ->
            val level = parseDagLevel(borough.dagLevel)
            DagNodeView(
                id = borough.name,
                label = "${borough.name} (${borough.dagLevel})",
                level = level,
                project = borough.project,
                group = borough.dagLevel
            )
        }
        if (boroughNodes.isNotEmpty()) return boroughNodes

        return data.dagNodes.map { node ->
            val level = parseDagLevel(node.dagLevel)
            DagNodeView(
                id = node.borough,
                label = "${node.borough} (${node.dagLevel})",
                level = level,
                project = node.project,
                group = node.dagLevel
            )
        }
    }

    internal fun parseDagLevel(raw: String): Int {
        return raw.trim().filter { it.isDigit() }.toIntOrNull() ?: 99
    }
}
