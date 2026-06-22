package dashboard

import dashboard.model.BoroughData
import dashboard.model.BoroughStatus
import dashboard.model.DashboardData
import dashboard.model.DagNode
import dashboard.model.EpicData
import dashboard.model.EpicStatus
import dashboard.model.SessionActivity
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

/**
 * Crawls INDEX.adoc and SESSIONS_HISTORY.adoc files from the configured
 * workspace directory and produces a typed [DashboardData] model.
 */
class IndexCrawler {

    fun crawlDirectory(root: Path): DashboardData {
        val indexFiles = mutableListOf<Path>()
        val sessionFiles = mutableListOf<Path>()
        Files.walkFileTree(root, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                when (file.fileName.toString()) {
                    "INDEX.adoc" -> indexFiles.add(file)
                    "SESSIONS_HISTORY.adoc" -> sessionFiles.add(file)
                }
                return FileVisitResult.CONTINUE
            }
        })
        val allBoroughs = mutableListOf<BoroughData>()
        val allNodes = mutableListOf<DagNode>()
        for (f in indexFiles) {
            val data = parseIndex(f)
            allBoroughs.addAll(data.boroughs)
            allNodes.addAll(data.dagNodes)
        }

        val allEpics = mutableListOf<EpicData>()
        for (f in indexFiles) {
            val data = parseIndex(f)
            val owner = extractBoroughFromPath(f, allBoroughs)
            allEpics.addAll(data.epics.map { it.copy(borough = owner ?: it.borough) })
        }

        val allSessions = mutableListOf<SessionActivity>()
        for (f in sessionFiles) {
            val sessions = crawlSessionsHistory(f)
            val borough = extractBoroughFromPath(f, allBoroughs)
            allSessions.addAll(sessions.map { it.copy(borough = borough ?: it.borough) })
        }
        return DashboardData(
            boroughs = allBoroughs,
            epics = allEpics,
            dagNodes = allNodes,
            sessions = allSessions
        )
    }

    fun crawlIndex(file: Path): DashboardData = parseIndex(file)

    private fun parseIndex(file: Path): DashboardData {
        val content = Files.readString(file)
        val tables = parseTables(content)
        val boroughs = tables.firstNotNullOfOrNull { parseBoroughTable(it) } ?: emptyList()
        val epics = tables.firstNotNullOfOrNull { parseEpicTable(it) } ?: emptyList()
        val nodes = boroughs.map { DagNode(borough = it.name, project = it.project, dagLevel = it.dagLevel) }
        return DashboardData(boroughs = boroughs, epics = epics, dagNodes = nodes)
    }

    fun crawlSessionsHistory(file: Path): List<SessionActivity> {
        val content = Files.readString(file)
        val tables = parseTables(content)
        return tables.firstNotNullOfOrNull { parseSessionsTable(it) } ?: emptyList()
    }

    private fun extractBoroughFromPath(file: Path, boroughs: List<BoroughData>): String? {
        val segments = file.map { it.toString().lowercase() }
        return boroughs.find { segments.contains(it.project.lowercase()) }?.name
            ?: boroughs.find { segments.contains(it.name.lowercase()) }?.name
    }

    private fun parseTables(content: String): List<List<List<String>>> {
        val tableRegex = """\|={3,}\n(.*?)\n\|={3,}""".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE))
        return tableRegex.findAll(content).map { match ->
            val body = match.groupValues[1].trim()
            body.lines()
                .map { line -> line.trim().trimStart('|').split('|').map { it.trim() } }
                .filter { row -> row.isNotEmpty() && row.all { cell -> cell.isNotBlank() } }
        }.toList()
    }

    private fun parseBoroughTable(rows: List<List<String>>): List<BoroughData>? {
        if (rows.isEmpty()) return null
        val header = rows.first().map { it.lowercase().trim() }
        val boroughIdx = header.indexOfFirst { it.contains("borough") }
        val projectIdx = header.indexOfFirst { it.contains("project") || it.contains("projet") }
        val dagIdx = header.indexOfFirst { it.contains("dag") }
        val roleIdx = header.indexOfFirst { it.contains("role") }
        val sessionIdx = header.indexOfFirst { it.contains("session") }
        if (boroughIdx == -1 || projectIdx == -1) return null

        return rows.drop(1).mapNotNull { row ->
            val project = projectIdx.takeIf { it >= 0 && it < row.size }?.let { row[it] } ?: ""
            if (project.isBlank() || project.contains("==")) return@mapNotNull null
            val role = roleIdx.takeIf { it >= 0 && it < row.size }?.let { row[it] } ?: ""
            val status = when {
                role.contains("VESTIGE", ignoreCase = true) -> BoroughStatus.VESTIGE
                role.contains("DORMANT", ignoreCase = true) -> BoroughStatus.DORMANT
                else -> BoroughStatus.ACTIVE
            }
            BoroughData(
                name = row.getOrElse(boroughIdx) { "" }.replace("==", "").trim(),
                project = project.replace("==", "").trim(),
                dagLevel = dagIdx.takeIf { it >= 0 && it < row.size }?.let { row[it] } ?: "",
                role = role,
                session = sessionIdx.takeIf { it >= 0 && it < row.size }?.let { row[it] } ?: "",
                status = status
            )
        }
    }

    private fun parseEpicTable(rows: List<List<String>>): List<EpicData>? {
        if (rows.isEmpty()) return null
        val header = rows.first().map { it.lowercase().trim() }
        val epicIdx = header.indexOfFirst { it.contains("epic") }
        val subjectIdx = header.indexOfFirst { it.contains("sujet") || it.contains("subject") }
        val ptsIdx = header.indexOfFirst { it.contains("pt") }
        val priorityIdx = header.indexOfFirst { it.contains("priorite") || it.contains("priority") }
        val statusIdx = header.indexOfFirst { it.contains("statut") || it.contains("status") }
        val boroughIdx = header.indexOfFirst { it.contains("borough") }
        if (epicIdx == -1) return null

        return rows.drop(1).map { row ->
            val rawStatus = row.getOrElse(statusIdx.takeIf { it >= 0 && it < row.size } ?: -1) { "" }
            EpicData(
                id = row.getOrElse(epicIdx) { "" },
                title = subjectIdx.takeIf { it >= 0 && it < row.size }?.let { row[it] } ?: "",
                borough = boroughIdx.takeIf { it >= 0 && it < row.size }?.let { row[it] } ?: "",
                points = ptsIdx.takeIf { it >= 0 && it < row.size }?.let { row[it].filter { c -> c.isDigit() }.toIntOrNull() } ?: 0,
                priority = priorityIdx.takeIf { it >= 0 && it < row.size }?.let { row[it] } ?: "P2",
                status = parseEpicStatus(rawStatus)
            )
        }
    }

    private fun parseSessionsTable(rows: List<List<String>>): List<SessionActivity>? {
        if (rows.isEmpty()) return null
        val header = rows.first().map { it.lowercase().trim() }
        val numIdx = header.indexOfFirst { it.contains("#") }
        val dateIdx = header.indexOfFirst { it.contains("date") }
        val typeIdx = header.indexOfFirst { it.contains("type") }
        val subjectIdx = header.indexOfFirst { it.contains("objet") || it.contains("object") || it.contains("sujet") || it.contains("subject") }
        val filesIdx = header.indexOfFirst { it.contains("fichiers") || it.contains("files") }
        if (numIdx == -1 || subjectIdx == -1) return null

        return rows.drop(1).map { row ->
            SessionActivity(
                number = row.getOrElse(numIdx) { "" },
                date = dateIdx.takeIf { it >= 0 && it < row.size }?.let { row[it] } ?: "",
                type = typeIdx.takeIf { it >= 0 && it < row.size }?.let { row[it] } ?: "",
                subject = row.getOrElse(subjectIdx) { "" },
                files = filesIdx.takeIf { it >= 0 && it < row.size }?.let { row[it] } ?: ""
            )
        }
    }

    private fun parseEpicStatus(raw: String): EpicStatus {
        return when {
            raw.contains("✅") || raw.contains("TERMINE", ignoreCase = true) ||
                raw.contains("DONE", ignoreCase = true) -> EpicStatus.TERMINE
            raw.contains("🔄") || raw.contains("EN COURS", ignoreCase = true) ||
                raw.contains("IN PROGRESS", ignoreCase = true) -> EpicStatus.EN_COURS
            raw.contains("BLOQUE", ignoreCase = true) ||
                raw.contains("BLOCKED", ignoreCase = true) -> EpicStatus.BLOQUE
            else -> EpicStatus.PLANIFIE
        }
    }
}
