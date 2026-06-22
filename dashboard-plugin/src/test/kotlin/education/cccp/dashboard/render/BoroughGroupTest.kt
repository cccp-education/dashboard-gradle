package education.cccp.dashboard.render

import education.cccp.dashboard.model.BoroughData
import education.cccp.dashboard.model.BoroughStatus
import education.cccp.dashboard.model.DashboardData
import education.cccp.dashboard.model.EpicData
import education.cccp.dashboard.model.EpicStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BoroughGroupTest {

    @Test
    fun `should group epics by borough sorted by dag level`() {
        val data = DashboardData(
            boroughs = listOf(
                BoroughData("BAKERY", "bakery-gradle", "N2", "Site", "S001"),
                BoroughData("Dashboard", "dashboard-gradle", "N3", "Vision", "S005")
            ),
            epics = listOf(
                EpicData("BKY-1", "Bakery task", "BAKERY", 5, "P1", EpicStatus.EN_COURS),
                EpicData("DSH-0", "Bootstrap", "Dashboard", 3, "P0", EpicStatus.TERMINE)
            ),
            dagNodes = emptyList()
        )

        val groups = BoroughGrouper.groupByBorough(data)

        assertThat(groups).hasSize(2)
        assertThat(groups.map { it.name }).containsExactly("BAKERY", "Dashboard")
        assertThat(groups[0].epics).extracting("id").containsExactly("BKY-1")
        assertThat(groups[1].epics).extracting("id").containsExactly("DSH-0")
    }

    @Test
    fun `should sort epics inside borough by priority then id`() {
        val data = DashboardData(
            boroughs = listOf(BoroughData("Dashboard", "dashboard-gradle", "N3", "Vision", "S005")),
            epics = listOf(
                EpicData("DSH-2", "Crawler", "Dashboard", 13, "P0", EpicStatus.PLANIFIE),
                EpicData("DSH-1", "Scaffold", "Dashboard", 8, "P0", EpicStatus.EN_COURS),
                EpicData("DSH-0", "Bootstrap", "Dashboard", 3, "P0", EpicStatus.TERMINE)
            ),
            dagNodes = emptyList()
        )

        val groups = BoroughGrouper.groupByBorough(data)

        assertThat(groups).hasSize(1)
        assertThat(groups[0].epics).extracting("id").containsExactly("DSH-0", "DSH-1", "DSH-2")
    }

    @Test
    fun `should expose borough status as string`() {
        val data = DashboardData(
            boroughs = listOf(BoroughData("Nashville", "newpipe-gradle", "N2", "VESTIGE", "S001", BoroughStatus.VESTIGE)),
            epics = listOf(EpicData("NP-1", "Auth", "Nashville", 5, "P2", EpicStatus.TERMINE)),
            dagNodes = emptyList()
        )

        val groups = BoroughGrouper.groupByBorough(data)

        assertThat(groups[0].status).isEqualTo("VESTIGE")
    }

    @Test
    fun `should create UNASSIGNED group for epics without borough`() {
        val data = DashboardData(
            boroughs = listOf(BoroughData("Dashboard", "dashboard-gradle", "N3", "Vision", "S005")),
            epics = listOf(
                EpicData("ORP-1", "Orphan epic", "", 2, "P2", EpicStatus.PLANIFIE),
                EpicData("DSH-0", "Bootstrap", "Dashboard", 3, "P0", EpicStatus.TERMINE)
            ),
            dagNodes = emptyList()
        )

        val groups = BoroughGrouper.groupByBorough(data)

        assertThat(groups).hasSize(2)
        val unassigned = groups.find { it.name == "UNASSIGNED" }
        assertThat(unassigned).isNotNull
        assertThat(unassigned!!.epics).hasSize(1)
        assertThat(unassigned.epics[0].id).isEqualTo("ORP-1")
    }
}
