package dashboard.timeline

/**
 * Aggregate representing the ordered sequence of workspace milestones.
 */
class Timeline(milestones: List<Milestone>) {

    val milestones: List<Milestone> = milestones.sorted()

    /**
     * Returns all milestones whose date is on or before [date] (inclusive).
     * Dates are compared lexicographically because they are expected to be
     * ISO-8601 formatted (YYYY-MM-DD).
     */
    fun milestonesUpTo(date: String): List<Milestone> =
        milestones.filter { it.date <= date }

    /**
     * Returns all milestones of the requested [type].
     */
    fun byType(type: MilestoneType): List<Milestone> =
        milestones.filter { it.type == type }
}
