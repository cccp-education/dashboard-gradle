package dashboard.timeline

import dashboard.model.BoroughData
import dashboard.model.DashboardData
import dashboard.model.EpicData
import dashboard.model.EpicStatus
import dashboard.model.SessionActivity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TimelineBuilderTest {

    @Test
    fun `build should create a milestone for each done epic dated by its session`() {
        val data = DashboardData(
            boroughs = listOf(BoroughData("Dashboard", "dashboard-gradle", "N3", "Vision", "S007")),
            epics = listOf(
                EpicData("DSH-0", "Bootstrap", "Dashboard", 3, "P0", EpicStatus.TERMINE, "000"),
                EpicData("DSH-1", "Plugin scaffold", "Dashboard", 8, "P0", EpicStatus.TERMINE, "001")
            ),
            dagNodes = emptyList(),
            sessions = listOf(
                SessionActivity("000", "2026-06-18", "gouvernance", "Bootstrap", "Dashboard", ""),
                SessionActivity("001", "2026-06-19", "build", "Plugin scaffold", "Dashboard", "")
            )
        )

        val timeline = TimelineBuilder.build(data)

        assertThat(timeline.milestones).hasSize(2)
        assertThat(timeline.milestones[0].id).isEqualTo("DSH-0")
        assertThat(timeline.milestones[0].date).isEqualTo("2026-06-18")
        assertThat(timeline.milestones[0].status).isEqualTo(MilestoneStatus.DONE)
        assertThat(timeline.milestones[1].id).isEqualTo("DSH-1")
        assertThat(timeline.milestones[1].date).isEqualTo("2026-06-19")
    }

    @Test
    fun `build should use planned future date when no session matches`() {
        val data = DashboardData(
            boroughs = listOf(BoroughData("BAKERY", "bakery-gradle", "N2", "Site", "S000")),
            epics = listOf(
                EpicData("BKY-1", "Bakery task", "BAKERY", 5, "P1", EpicStatus.PLANIFIE, "")
            ),
            dagNodes = emptyList(),
            sessions = emptyList()
        )

        val timeline = TimelineBuilder.build(data)

        assertThat(timeline.milestones).hasSize(1)
        assertThat(timeline.milestones[0].id).isEqualTo("BKY-1")
        assertThat(timeline.milestones[0].date).isEqualTo(TimelineBuilder.DEFAULT_PLANNED_DATE)
        assertThat(timeline.milestones[0].status).isEqualTo(MilestoneStatus.PLANNED)
    }

    @Test
    fun `build should map in-progress epic to milestone status in progress`() {
        val data = DashboardData(
            boroughs = listOf(BoroughData("Dashboard", "dashboard-gradle", "N3", "Vision", "S007")),
            epics = listOf(
                EpicData("DSH-7", "Integration", "Dashboard", 5, "P1", EpicStatus.EN_COURS, "008")
            ),
            dagNodes = emptyList(),
            sessions = listOf(
                SessionActivity("008", "2026-06-23", "implementation", "Integration", "Dashboard", "")
            )
        )

        val timeline = TimelineBuilder.build(data)

        assertThat(timeline.milestones).hasSize(1)
        assertThat(timeline.milestones[0].status).isEqualTo(MilestoneStatus.IN_PROGRESS)
        assertThat(timeline.milestones[0].date).isEqualTo("2026-06-23")
    }

    @Test
    fun `build should skip epics without borough`() {
        val data = DashboardData(
            boroughs = emptyList(),
            epics = listOf(
                EpicData("DSH-0", "Bootstrap", "", 3, "P0", EpicStatus.TERMINE, "000")
            ),
            dagNodes = emptyList(),
            sessions = listOf(SessionActivity("000", "2026-06-18", "gouvernance", "Bootstrap", "", ""))
        )

        val timeline = TimelineBuilder.build(data)

        assertThat(timeline.milestones).isEmpty()
    }

    @Test
    fun `build should include borough dag level in milestone`() {
        val data = DashboardData(
            boroughs = listOf(BoroughData("BAKERY", "bakery-gradle", "N2", "Site", "S001")),
            epics = listOf(
                EpicData("BKY-1", "Bakery task", "BAKERY", 5, "P1", EpicStatus.TERMINE, "001")
            ),
            dagNodes = emptyList(),
            sessions = listOf(SessionActivity("001", "2026-06-20", "build", "Bakery", "BAKERY", ""))
        )

        val timeline = TimelineBuilder.build(data)

        assertThat(timeline.milestones[0].dagLevel).isEqualTo("N2")
    }
}
