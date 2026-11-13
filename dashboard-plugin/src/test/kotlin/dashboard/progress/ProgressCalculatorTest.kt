package dashboard.progress

import dashboard.model.BoroughData
import dashboard.model.EpicData
import dashboard.model.EpicStatus
import dashboard.model.PointTotals
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ProgressCalculatorTest {
    @Test
    fun `computes point-based progress per borough and globally`() {
        val boroughs =
            listOf(
                BoroughData("Dashboard", "dashboard-gradle", "N3", "", ""),
                BoroughData("BAKERY", "bakery-gradle", "N2", "", ""),
            )
        val epics =
            listOf(
                EpicData("DSH-0", "Done", "Dashboard", points = 3, status = EpicStatus.TERMINE),
                EpicData("DSH-1", "Doing", "Dashboard", points = 5, status = EpicStatus.EN_COURS),
                EpicData("DSH-2", "Planned", "Dashboard", points = 2, status = EpicStatus.PLANIFIE),
                EpicData("BKY-1", "Done", "BAKERY", points = 4, status = EpicStatus.TERMINE),
                EpicData("BKY-2", "Blocked", "BAKERY", points = 6, status = EpicStatus.BLOQUE),
            )

        val progress = ProgressCalculator.compute(boroughs, epics)

        assertThat(progress.donePoints).isEqualTo(7)
        assertThat(progress.totalPoints).isEqualTo(20)
        assertThat(progress.percent).isEqualTo(35)
        assertThat(progress.doneEpics).isEqualTo(2)
        assertThat(progress.totalEpics).isEqualTo(5)

        val dashboard = progress.byBorough.first { it.borough == "Dashboard" }
        assertThat(dashboard.donePoints).isEqualTo(3)
        assertThat(dashboard.totalPoints).isEqualTo(10)
        assertThat(dashboard.percent).isEqualTo(30)

        val bakery = progress.byBorough.first { it.borough == "BAKERY" }
        assertThat(bakery.donePoints).isEqualTo(4)
        assertThat(bakery.totalPoints).isEqualTo(10)
        assertThat(bakery.percent).isEqualTo(40)
    }

    @Test
    fun `zero total points yields zero percent`() {
        val borough = BoroughData("Dashboard", "dashboard-gradle", "N3", "", "")
        val epics = listOf(EpicData("DSH-0", "Bootstrap", "Dashboard", points = 0, status = EpicStatus.TERMINE))

        val progress = ProgressCalculator.compute(listOf(borough), epics)

        assertThat(progress.totalPoints).isZero()
        assertThat(progress.percent).isZero()
        assertThat(progress.doneEpics).isEqualTo(1)
        assertThat(progress.byBorough.single().percent).isZero()
    }

    @Test
    fun `keeps boroughs without epics at zero`() {
        val boroughs =
            listOf(
                BoroughData("Dashboard", "dashboard-gradle", "N3", "", ""),
                BoroughData("Empty", "empty-gradle", "N2", "", ""),
            )

        val progress =
            ProgressCalculator.compute(
                boroughs,
                listOf(EpicData("DSH-0", "Done", "Dashboard", points = 3, status = EpicStatus.TERMINE)),
            )

        val empty = progress.byBorough.first { it.borough == "Empty" }
        assertThat(empty.totalEpics).isZero()
        assertThat(empty.totalPoints).isZero()
        assertThat(empty.percent).isZero()
    }

    @Test
    fun `backlog point totals override the epic roadmap`() {
        val borough = BoroughData("Dashboard", "dashboard-gradle", "N3", "", "")
        val epics = listOf(EpicData("DSH-0", "Done", "Dashboard", points = 0, status = EpicStatus.TERMINE))

        val progress =
            ProgressCalculator.compute(
                listOf(borough),
                epics,
                mapOf("Dashboard" to PointTotals(donePoints = 10, totalPoints = 20)),
            )

        assertThat(progress.donePoints).isEqualTo(10)
        assertThat(progress.totalPoints).isEqualTo(20)
        assertThat(progress.percent).isEqualTo(50)
        assertThat(progress.byBorough.single().doneEpics).isEqualTo(1)
    }

    @Test
    fun `counts orphan epics that have no borough row`() {
        val progress =
            ProgressCalculator.compute(
                listOf(BoroughData("Dashboard", "dashboard-gradle", "N3", "", "")),
                listOf(
                    EpicData("DSH-0", "Done", "Dashboard", points = 5, status = EpicStatus.TERMINE),
                    EpicData("GBL-1", "Global", "Transverse", points = 5, status = EpicStatus.PLANIFIE),
                ),
            )

        assertThat(progress.totalPoints).isEqualTo(10)
        assertThat(progress.donePoints).isEqualTo(5)
        assertThat(progress.percent).isEqualTo(50)
        assertThat(progress.byBorough.map { it.borough }).containsExactly("Dashboard", "Transverse")
    }
}
