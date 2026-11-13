package dashboard.progress

import dashboard.model.BoroughData
import dashboard.model.BoroughProgress
import dashboard.model.EpicData
import dashboard.model.EpicStatus
import dashboard.model.ProgressSummary
import java.util.LinkedHashMap
import kotlin.math.roundToInt

/**
 * Computes point-based progress (`terminés / total`) per borough and globally.
 *
 * Points are the only comparable unit across boroughs: EPIC counts vary by
 * granularity while declared points reflect the estimated effort. An EPIC with
 * zero declared points still counts in `totalEpics` but adds nothing to the
 * point totals.
 */
object ProgressCalculator {
    fun compute(
        boroughs: List<BoroughData>,
        epics: List<EpicData>,
    ): ProgressSummary {
        val epicsByBorough = epics.groupBy { it.borough }
        val entries = LinkedHashMap<String, BoroughProgress>()

        for (borough in boroughs) {
            entries[borough.name] = progressOf(borough.name, borough.project, epicsByBorough[borough.name].orEmpty())
        }
        for ((name, boroughEpics) in epicsByBorough) {
            entries.putIfAbsent(name, progressOf(name, "", boroughEpics))
        }

        val byBorough = entries.values.toList()
        val donePoints = epics.filter { it.status == EpicStatus.TERMINE }.sumOf { it.points }
        val totalPoints = epics.sumOf { it.points }
        return ProgressSummary(
            donePoints = donePoints,
            totalPoints = totalPoints,
            percent = percentOf(donePoints, totalPoints),
            doneEpics = epics.count { it.status == EpicStatus.TERMINE },
            totalEpics = epics.size,
            byBorough = byBorough,
        )
    }

    private fun progressOf(
        borough: String,
        project: String,
        epics: List<EpicData>,
    ): BoroughProgress {
        val done = epics.filter { it.status == EpicStatus.TERMINE }
        val donePoints = done.sumOf { it.points }
        val totalPoints = epics.sumOf { it.points }
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
