package dashboard.model

data class DashboardData(
    val boroughs: List<BoroughData>,
    val epics: List<EpicData>,
    val dagNodes: List<DagNode>,
    val sessions: List<SessionActivity> = emptyList(),
    val timestamp: String = java.time.Instant.now().toString()
)

data class SessionActivity(
    val number: String,
    val date: String,
    val type: String,
    val subject: String,
    val borough: String = "",
    val files: String = ""
)

data class BoroughData(
    val name: String,
    val project: String,
    val dagLevel: String,
    val role: String,
    val session: String,
    val status: BoroughStatus = BoroughStatus.ACTIVE
)

enum class BoroughStatus { ACTIVE, VESTIGE, DORMANT }

data class EpicData(
    val id: String,
    val title: String,
    val borough: String,
    val points: Int = 0,
    val priority: String = "P2",
    val status: EpicStatus = EpicStatus.PLANIFIE,
    val session: String = ""
)

enum class EpicStatus {
    TERMINE,
    EN_COURS,
    PLANIFIE,
    BLOQUE
}

data class DagNode(
    val borough: String,
    val project: String,
    val dagLevel: String,
    val children: List<String> = emptyList()
)
