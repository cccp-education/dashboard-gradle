package dashboard

import java.io.Serializable

/**
 * One ring of the MVP0 critical path (A1 → A4).
 *
 * [projects] lists the crawled workspace projects that belong to the ring; the
 * renderer aggregates their point progress. [state] is optional: when blank the
 * state is derived from the aggregated progress.
 */
data class CriticalPathRing(
    val ring: String,
    val label: String,
    val projects: List<String>,
    val state: String = "",
) : Serializable
