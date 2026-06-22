package dashboard.render

import dashboard.model.BoroughData
import dashboard.model.DashboardData
import dashboard.model.DagNode
import dashboard.model.EpicData
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DagGraphBuilderTest {

    @Test
    fun `build should create nodes from borough data`() {
        val data = DashboardData(
            boroughs = listOf(
                BoroughData("MEMPHIS", "workspace-bom", "N0", "BOM", "S005"),
                BoroughData("Dashboard", "dashboard-gradle", "N3", "Vision", "S006")
            ),
            epics = emptyList(),
            dagNodes = emptyList()
        )

        val graph = DagGraphBuilder.build(data)

        assertThat(graph.nodes).hasSize(2)
        assertThat(graph.nodes.map { it.id }).containsExactlyInAnyOrder("MEMPHIS", "Dashboard")
        assertThat(graph.nodes.find { it.id == "MEMPHIS" }?.level).isEqualTo(0)
        assertThat(graph.nodes.find { it.id == "Dashboard" }?.level).isEqualTo(3)
    }

    @Test
    fun `build should create edges between consecutive DAG levels`() {
        val data = DashboardData(
            boroughs = listOf(
                BoroughData("N0-A", "n0-a-gradle", "N0", "Base", "S000"),
                BoroughData("N1-A", "n1-a-gradle", "N1", "Layer 1", "S001"),
                BoroughData("N2-A", "n2-a-gradle", "N2", "Layer 2", "S002")
            ),
            epics = emptyList(),
            dagNodes = emptyList()
        )

        val graph = DagGraphBuilder.build(data)

        val edges = graph.edges
        assertThat(edges).hasSize(2)
        assertThat(edges).anyMatch { it.from == "N0-A" && it.to == "N1-A" }
        assertThat(edges).anyMatch { it.from == "N1-A" && it.to == "N2-A" }
    }

    @Test
    fun `build should create multiple edges when several nodes depend on all previous level nodes`() {
        val data = DashboardData(
            boroughs = listOf(
                BoroughData("N0-A", "n0-a-gradle", "N0", "Base", "S000"),
                BoroughData("N0-B", "n0-b-gradle", "N0", "Base", "S000"),
                BoroughData("N1-A", "n1-a-gradle", "N1", "Layer 1", "S001")
            ),
            epics = emptyList(),
            dagNodes = emptyList()
        )

        val graph = DagGraphBuilder.build(data)

        assertThat(graph.edges).hasSize(2)
        assertThat(graph.edges.map { it.from to it.to }).containsExactlyInAnyOrder(
            "N0-A" to "N1-A",
            "N0-B" to "N1-A"
        )
    }

    @Test
    fun `build should handle empty data`() {
        val graph = DagGraphBuilder.build(DashboardData(emptyList(), emptyList(), emptyList()))

        assertThat(graph.nodes).isEmpty()
        assertThat(graph.edges).isEmpty()
    }

    @Test
    fun `build should fallback to dagNodes when boroughs are empty`() {
        val data = DashboardData(
            boroughs = emptyList(),
            epics = emptyList(),
            dagNodes = listOf(
                DagNode("X", "x-gradle", "N2"),
                DagNode("Y", "y-gradle", "N3")
            )
        )

        val graph = DagGraphBuilder.build(data)

        assertThat(graph.nodes).hasSize(2)
        assertThat(graph.edges).hasSize(1)
        assertThat(graph.edges[0].from).isEqualTo("X")
        assertThat(graph.edges[0].to).isEqualTo("Y")
    }
}
