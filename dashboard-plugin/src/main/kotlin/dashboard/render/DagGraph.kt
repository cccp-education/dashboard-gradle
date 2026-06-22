package dashboard.render

/**
 * DAG graph model exposed to the template for vis.js rendering.
 *
 * Nodes represent boroughs, edges represent dependency between consecutive
 * DAG levels (N0 → N1 → N2 → N3 → N4).
 */
data class DagGraph(
    val nodes: List<DagNodeView>,
    val edges: List<DagEdgeView>
)

data class DagNodeView(
    val id: String,
    val label: String,
    val level: Int,
    val project: String,
    val group: String
)

data class DagEdgeView(
    val from: String,
    val to: String
)
