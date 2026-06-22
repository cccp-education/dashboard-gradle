package dashboard.timeline

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MilestoneTest {

    @Test
    fun `milestone should expose its properties`() {
        val milestone = Milestone(
            id = "DSH-0",
            label = "Bootstrap governance",
            date = "2026-06-18",
            borough = "Dashboard",
            dagLevel = "N3",
            type = MilestoneType.EPIC_RELEASE,
            status = MilestoneStatus.DONE
        )

        assertThat(milestone.id).isEqualTo("DSH-0")
        assertThat(milestone.label).isEqualTo("Bootstrap governance")
        assertThat(milestone.date).isEqualTo("2026-06-18")
        assertThat(milestone.borough).isEqualTo("Dashboard")
        assertThat(milestone.dagLevel).isEqualTo("N3")
        assertThat(milestone.type).isEqualTo(MilestoneType.EPIC_RELEASE)
        assertThat(milestone.status).isEqualTo(MilestoneStatus.DONE)
    }

    @Test
    fun `milestones should be ordered by date then by id`() {
        val later = Milestone(
            id = "DSH-1",
            label = "Plugin scaffold",
            date = "2026-06-20",
            borough = "Dashboard",
            dagLevel = "N3",
            type = MilestoneType.EPIC_RELEASE,
            status = MilestoneStatus.DONE
        )
        val earlier = Milestone(
            id = "DSH-0",
            label = "Bootstrap governance",
            date = "2026-06-18",
            borough = "Dashboard",
            dagLevel = "N3",
            type = MilestoneType.EPIC_RELEASE,
            status = MilestoneStatus.DONE
        )

        assertThat(listOf(later, earlier).sorted()).containsExactly(earlier, later)
    }

    @Test
    fun `milestones with same date should be ordered by id`() {
        val b = Milestone(
            id = "BKY-1",
            label = "Bakery task",
            date = "2026-06-18",
            borough = "BAKERY",
            dagLevel = "N2",
            type = MilestoneType.EPIC_RELEASE,
            status = MilestoneStatus.DONE
        )
        val a = Milestone(
            id = "DSH-0",
            label = "Bootstrap governance",
            date = "2026-06-18",
            borough = "Dashboard",
            dagLevel = "N3",
            type = MilestoneType.EPIC_RELEASE,
            status = MilestoneStatus.DONE
        )

        assertThat(listOf(b, a).sorted()).containsExactly(b, a)
    }
}
