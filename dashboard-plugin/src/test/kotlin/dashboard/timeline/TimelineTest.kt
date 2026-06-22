package dashboard.timeline

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TimelineTest {

    private fun milestone(id: String, date: String, type: MilestoneType = MilestoneType.EPIC_RELEASE) = Milestone(
        id = id,
        label = "Label $id",
        date = date,
        borough = "Dashboard",
        dagLevel = "N3",
        type = type,
        status = MilestoneStatus.DONE
    )

    @Test
    fun `timeline should keep milestones sorted by date`() {
        val m1 = milestone("M1", "2026-06-20")
        val m2 = milestone("M2", "2026-06-18")
        val m3 = milestone("M3", "2026-06-19")

        val timeline = Timeline(listOf(m1, m2, m3))

        assertThat(timeline.milestones).containsExactly(m2, m3, m1)
    }

    @Test
    fun `milestonesUpTo should return only milestones before or on the given date`() {
        val m1 = milestone("M1", "2026-06-18")
        val m2 = milestone("M2", "2026-06-20")
        val m3 = milestone("M3", "2026-06-22")

        val timeline = Timeline(listOf(m1, m2, m3))

        assertThat(timeline.milestonesUpTo("2026-06-19")).containsExactly(m1)
        assertThat(timeline.milestonesUpTo("2026-06-20")).containsExactly(m1, m2)
        assertThat(timeline.milestonesUpTo("2026-06-25")).containsExactly(m1, m2, m3)
    }

    @Test
    fun `byType should filter milestones by type`() {
        val epic = milestone("E1", "2026-06-18", MilestoneType.EPIC_RELEASE)
        val session = Milestone(
            id = "S1",
            label = "Session 1",
            date = "2026-06-18",
            borough = "Dashboard",
            dagLevel = "N3",
            type = MilestoneType.SESSION,
            status = MilestoneStatus.DONE
        )

        val timeline = Timeline(listOf(epic, session))

        assertThat(timeline.byType(MilestoneType.EPIC_RELEASE)).containsExactly(epic)
        assertThat(timeline.byType(MilestoneType.SESSION)).containsExactly(session)
    }
}
