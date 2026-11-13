package dashboard.progress

import dashboard.model.BoroughData
import dashboard.model.BoroughProgress
import dashboard.model.EpicData
import dashboard.model.EpicStatus
import dashboard.model.PointTotals
import dashboard.model.ProgressSummary
import java.util.LinkedHashMap
import kotlin.math.roundToInt

/**
 * Computes point-based progress (`terminés / total`) per borough and globally.
 *
 * Points are the only comparable unit across boroughs: EPIC counts vary by
 * granularity while declared points reflect the estimated effort. When a
 * borough `BACKLOG.adoc` exposes point totals, those take precedence over the
 * coarser EPIC roadmap (some INDEX roadmaps have no `Pts` column).
 */
object ProgressCalculator {
    fun compute(
        boroughs: List<BoroughData>,
        epics: List<EpicData>,
        backlogPoints: Map<String, PointTotals> = emptyMap(),
    ): ProgressSummary {
        val epicsByBorough = epics.groupBy { it.borough }
        val entries = LinkedHashMap<String, BoroughProgress>()

        for (borough in boroughs) {
            entries[borough.name] =
                progressOf(
                    borough = borough.name,
                    project = borough.project,
                    epics = epicsByBorough[borough.name].orEmpty(),
                    backlogPoints = backlogPoints[borough.name],
                )
        }
        for ((name, boroughEpics) in epicsByBorough) {
            entries.putIfAbsent(name, progressOf(name, "", boroughEpics, backlogPoints[name]))
        }

        val byBorough = entries.values.toList()
        val donePoints = byBorough.sumOf { it.donePoints }
        val totalPoints = byBorough.sumOf { it.totalPoints }
        return ProgressSummary(
            donePoints = donePoints,
            totalPoints = totalPoints,
            percent = percentOf(donePoints, totalPoints),
            doneEpics = byBorough.sumOf { it.doneEpics },
            totalEpics = byBorough.sumOf { it.totalEpics },
            byBorough = byBorough,
        )
    }

    private fun progressOf(
        borough: String,
        project: String,
        epics: List<EpicData>,
        backlogPoints: PointTotals?,
    ): BoroughProgress {
        val done = epics.filter { it.status == EpicStatus.TERMINE }
        val donePoints = backlogPoints?.donePoints ?: done.sumOf { it.points }
        val totalPoints = backlogPoints?.totalPoints ?: epics.sumOf { it.points }
        return BoroughProgress(
            borough = borough,
            project = project,
            donePoints = donePoints,
            totalPoints = totalPoints,
            percent = percentOf(donePoints, totalPoints),
            doneEpics = done.size,
            totalEpics = epics.size,
        )
    }

    private fun percentOf(
        done: Int,
        total: Int,
    ): Int = if (total <= 0) 0 else (done * 100.0 / total).roundToInt().coerceIn(0, 100)
}
