package dashboard.timeline

enum class MilestoneType {
    EPIC_RELEASE,
    SESSION,
    SPRINT_GATE
}

enum class MilestoneStatus {
    DONE,
    IN_PROGRESS,
    PLANNED
}

/**
 * A milestone on the workspace timeline.
 *
 * Milestones are immutable value objects ordered chronologically by [date],
 * then by [id] for stable sorting.
 */
data class Milestone(
    val id: String,
    val label: String,
    val date: String,
    val borough: String,
    val dagLevel: String,
    val type: MilestoneType,
    val status: MilestoneStatus
) : Comparable<Milestone> {

    override fun compareTo(other: Milestone): Int {
        return compareValuesBy(this, other, { it.date }, { it.id })
    }
}
