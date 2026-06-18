package education.cccp.dashboard

import education.cccp.dashboard.model.BoroughData
import education.cccp.dashboard.model.BoroughStatus
import education.cccp.dashboard.model.DashboardData
import education.cccp.dashboard.model.DagNode
import education.cccp.dashboard.model.EpicData
import education.cccp.dashboard.model.EpicStatus
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

class IndexCrawler {

    fun crawlDirectory(root: Path): DashboardData {
        val indexFiles = mutableListOf<Path>()
        Files.walkFileTree(root, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (file.fileName.toString() == "INDEX.adoc") {
                    indexFiles.add(file)
                }
                return FileVisitResult.CONTINUE
            }
        })
        val allBoroughs = mutableListOf<BoroughData>()
        val allEpics = mutableListOf<EpicData>()
        val allNodes = mutableListOf<DagNode>()
        for (f in indexFiles) {
            val data = crawlIndex(f)
            allBoroughs.addAll(data.boroughs)
            allEpics.addAll(data.epics)
            allNodes.addAll(data.dagNodes)
        }
        return DashboardData(
            boroughs = allBoroughs,
            epics = allEpics,
            dagNodes = allNodes
        )
    }

    fun crawlIndex(file: Path): DashboardData {
        val content = Files.readString(file)
        val tables = parseTables(content)
        val boroughs = tables.firstNotNullOfOrNull { parseBoroughTable(it) } ?: emptyList()
        val epics = tables.firstNotNullOfOrNull { parseEpicTable(it) } ?: emptyList()
        val nodes = boroughs.map { DagNode(borough = it.name, project = it.project, dagLevel = it.dagLevel) }
        return DashboardData(boroughs = boroughs, epics = epics, dagNodes = nodes)
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
        val projectIdx = header.indexOfFirst { it.contains("project") }
        val dagIdx = header.indexOfFirst { it.contains("dag") }
        val roleIdx = header.indexOfFirst { it.contains("role") }
        val sessionIdx = header.indexOfFirst { it.contains("session") }
        if (boroughIdx == -1) return null

        return rows.drop(1).map { row ->
            val role = roleIdx.takeIf { it >= 0 && it < row.size }?.let { row[it] } ?: ""
            val status = when {
                role.contains("VESTIGE", ignoreCase = true) -> BoroughStatus.VESTIGE
                role.contains("DORMANT", ignoreCase = true) -> BoroughStatus.DORMANT
                else -> BoroughStatus.ACTIVE
            }
            BoroughData(
                name = row.getOrElse(boroughIdx) { "" },
                project = projectIdx.takeIf { it >= 0 && it < row.size }?.let { row[it] } ?: "",
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
        val sujetIdx = header.indexOfFirst { it.contains("sujet") || it.contains("subject") }
        val ptsIdx = header.indexOfFirst { it.contains("pt") }
        val priorityIdx = header.indexOfFirst { it.contains("priorite") || it.contains("priority") }
        val statusIdx = header.indexOfFirst { it.contains("statut") || it.contains("status") }
        if (epicIdx == -1) return null

        return rows.drop(1).map { row ->
            val rawStatus = row.getOrElse(statusIdx.takeIf { it >= 0 && it < row.size } ?: -1) { "" }
            EpicData(
                id = row.getOrElse(epicIdx) { "" },
                title = sujetIdx.takeIf { it >= 0 && it < row.size }?.let { row[it] } ?: "",
                borough = "",
                points = ptsIdx.takeIf { it >= 0 && it < row.size }?.let { row[it].filter { c -> c.isDigit() }.toIntOrNull() } ?: 0,
                priority = priorityIdx.takeIf { it >= 0 && it < row.size }?.let { row[it] } ?: "P2",
                status = parseEpicStatus(rawStatus)
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
