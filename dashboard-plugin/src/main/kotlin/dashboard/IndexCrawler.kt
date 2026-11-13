package dashboard

import dashboard.model.BoroughData
import dashboard.model.BoroughStatus
import dashboard.model.DagNode
import dashboard.model.DashboardData
import dashboard.model.EpicData
import dashboard.model.EpicStatus
import dashboard.model.PointTotals
import dashboard.model.SessionActivity
import dashboard.progress.ProgressCalculator
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

/**
 * Crawls INDEX.adoc and SESSIONS_HISTORY.adoc files from the configured
 * workspace directory and produces a typed [DashboardData] model.
 *
 * Directories that only contain build output, dependency caches or historical
 * backups are pruned so that crawling the whole `foundry/` tree stays fast and
 * does not report duplicated governance entries.
 */
class IndexCrawler {
    fun crawlDirectory(
        root: Path,
        workspaceBacklog: Path? = null,
    ): DashboardData {
        val indexFiles = mutableListOf<Path>()
        val sessionFiles = mutableListOf<Path>()
        val backlogFiles = mutableListOf<Path>()
        Files.walkFileTree(
            root,
            object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(
                    dir: Path,
                    attrs: BasicFileAttributes,
                ): FileVisitResult {
                    val name = dir.fileName?.toString() ?: return FileVisitResult.CONTINUE
                    val skipped = dir != root && name in SKIPPED_DIRECTORIES
                    return if (skipped) FileVisitResult.SKIP_SUBTREE else FileVisitResult.CONTINUE
                }

                override fun visitFile(
                    file: Path,
                    attrs: BasicFileAttributes,
                ): FileVisitResult {
                    when (file.fileName.toString()) {
                        INDEX_FILE -> indexFiles.add(file)
                        SESSIONS_FILE -> sessionFiles.add(file)
                        BACKLOG_FILE -> backlogFiles.add(file)
                    }
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(
                    file: Path,
                    exc: IOException,
                ): FileVisitResult = FileVisitResult.CONTINUE
            },
        )
        indexFiles.sort()
        sessionFiles.sort()
        backlogFiles.sort()

        val boroughs = LinkedHashMap<String, BoroughData>()
        val epics = LinkedHashMap<String, EpicData>()
        for (file in indexFiles) {
            val data = parseIndex(file)
            val fileBoroughs =
                data.boroughs.ifEmpty {
                    if (isAlreadyRepresented(root, file, boroughs.values)) {
                        emptyList()
                    } else {
                        synthesizeBorough(root, file)?.let { listOf(it) } ?: emptyList()
                    }
                }
            for (borough in fileBoroughs) {
                boroughs.merge(boroughKey(borough), borough, ::mergeBorough)
            }
            val owner = extractBoroughFromPath(file, boroughs.values.toList())
            for (epic in data.epics) {
                val enriched = epic.copy(borough = epic.borough.ifBlank { owner ?: "" })
                epics.merge(epicKey(enriched), enriched, ::mergeEpic)
            }
        }

        val sessions = LinkedHashMap<String, SessionActivity>()
        for (file in sessionFiles) {
            val owner = extractBoroughFromPath(file, boroughs.values.toList())
            for (session in crawlSessionsHistory(file)) {
                val enriched = session.copy(borough = session.borough.ifBlank { owner ?: "" })
                sessions.merge(sessionKey(enriched), enriched, ::mergeSession)
            }
        }

        val backlogPoints = LinkedHashMap<String, PointTotals>()
        for (file in backlogFiles) {
            if (workspaceBacklog != null && isSameFile(file, workspaceBacklog)) continue
            val owner = extractBoroughFromPath(file, boroughs.values.toList()) ?: continue
            val totals = parseBacklogPoints(file) ?: continue
            if (totals.totalPoints <= 0) continue
            backlogPoints.merge(owner, totals) { current, candidate ->
                if (candidate.totalPoints > current.totalPoints) candidate else current
            }
        }

        if (workspaceBacklog != null && Files.isRegularFile(workspaceBacklog)) {
            parseWorkspaceBacklog(workspaceBacklog)?.let { (borough, globalEpics) ->
                boroughs.merge(boroughKey(borough), borough, ::mergeBorough)
                for (epic in globalEpics) {
                    epics.merge(epicKey(epic), epic, ::mergeEpic)
                }
            }
        }
        val finalBoroughs = boroughs.values.toList()
        val finalEpics = epics.values.toList()
        return DashboardData(
            boroughs = finalBoroughs,
            epics = finalEpics,
            dagNodes =
                finalBoroughs.map {
                    DagNode(borough = it.name, project = it.project, dagLevel = it.dagLevel)
                },
            sessions = sessions.values.toList(),
            progress = ProgressCalculator.compute(finalBoroughs, finalEpics, backlogPoints),
        )
    }

    fun crawlIndex(file: Path): DashboardData = parseIndex(file)

    fun crawlSessionsHistory(file: Path): List<SessionActivity> {
        val content = Files.readString(file)
        val tables = parseTables(content)
        return tables.firstNotNullOfOrNull { parseSessionsTable(it) } ?: emptyList()
    }

    private fun parseIndex(file: Path): DashboardData {
        val content = Files.readString(file)
        val tables = parseTables(content)
        val boroughs = tables.firstNotNullOfOrNull { parseBoroughTable(it) } ?: emptyList()
        val epics = tables.firstNotNullOfOrNull { parseEpicTable(it) } ?: emptyList()
        val nodes = boroughs.map { DagNode(borough = it.name, project = it.project, dagLevel = it.dagLevel) }
        return DashboardData(
            boroughs = boroughs,
            epics = epics,
            dagNodes = nodes,
            progress = ProgressCalculator.compute(boroughs, epics),
        )
    }

    /**
     * Parses point totals from a borough `BACKLOG.adoc`.
     *
     * Only tables that declare a points column are considered. EPIC-level
     * tables win over US-level tables (an EPIC table is already the sum of its
     * US), then every retained row contributes its points and, when a status
     * column exists, its terminal state.
     */
    private fun parseBacklogPoints(file: Path): PointTotals? {
        val tables = parseTables(Files.readString(file))
        val withPoints =
            tables.filter { rows ->
                val firstRow = rows.firstOrNull()
                val header = firstRow?.map { it.lowercase().trim() } ?: emptyList()
                header.any { it.isPointsHeader() }
            }
        if (withPoints.isEmpty()) return null
        val epicTables =
            withPoints.filter { rows ->
                val header = rows.first().map { it.lowercase().trim() }
                val firstCell = header.firstOrNull().orEmpty()
                firstCell.contains("epic") && header.any { it.isStatusHeader() }
            }
        val selected = if (epicTables.isNotEmpty()) epicTables else withPoints

        var done = 0
        var total = 0
        for (table in selected) {
            val header = table.first().map { it.lowercase().trim() }
            val ptsIdx = header.indexOfFirst { it.isPointsHeader() }
            val statusIdx = header.indexOfFirst { it.isStatusHeader() }
            for (row in table.drop(1)) {
                val points = ptsIdx.takeIf { it in row.indices }?.let { parsePoints(row[it]) } ?: 0
                if (points <= 0) continue
                total += points
                if (statusIdx in row.indices && isDoneCell(row[statusIdx])) done += points
            }
        }
        return if (total > 0) PointTotals(donePoints = done, totalPoints = total) else null
    }

    private fun isDoneCell(cell: String): Boolean {
        val lower = cell.lowercase()
        return cell.contains("✅") || lower.contains("termin") || lower.contains("done")
    }

    private fun isSameFile(
        a: Path,
        b: Path,
    ): Boolean = runCatching { Files.isSameFile(a, b) }.getOrDefault(false)

    /**
     * Parses the workspace-root `BACKLOG.adoc` (GBL transverse EPICs).
     *
     * The global backlog uses a different status vocabulary than borough
     * roadmaps (`✅ CADRÉ` means "cadrage done", not "EPIC done"), so the
     * terminal state is only `TERMINÉ` / `DONE`.
     */
    private fun parseWorkspaceBacklog(file: Path): Pair<BoroughData, List<EpicData>>? {
        val content = Files.readString(file)
        val table =
            parseTables(content).firstOrNull { rows ->
                val header = rows.firstOrNull()?.map { it.lowercase() }.orEmpty()
                header.any { it.contains("epic") } && header.any { it.isPointsHeader() }
            } ?: return null
        val header = table.first().map { it.lowercase().trim() }
        val epicIdx = header.indexOfFirst { it.contains("epic") }
        val subjectIdx = header.indexOfFirst { it.contains("sujet") || it.contains("subject") }
        val ptsIdx = header.indexOfFirst { it.isPointsHeader() }
        val statusIdx = header.indexOfFirst { it.contains("statut") || it.contains("status") }
        if (epicIdx == -1) return null

        val epics =
            table.drop(1).mapNotNull { row ->
                val id = row.getOrElse(epicIdx) { "" }.trim()
                if (id.isBlank()) return@mapNotNull null
                EpicData(
                    id = id,
                    title = subjectIdx.takeIf { it >= 0 && it < row.size }?.let { row[it] } ?: "",
                    borough = TRANSVERSE_BOROUGH,
                    points = ptsIdx.takeIf { it >= 0 && it < row.size }?.let { parsePoints(row[it]) } ?: 0,
                    priority = "GBL",
                    status = parseBacklogStatus(row.getOrElse(statusIdx.takeIf { it >= 0 && it < row.size } ?: -1) { "" }),
                )
            }
        val borough =
            BoroughData(
                name = TRANSVERSE_BOROUGH,
                project = TRANSVERSE_PROJECT,
                dagLevel = "GBL",
                role = "EPICs globaux (BACKLOG racine)",
                session = "",
            )
        return borough to epics
    }

    private fun parseBacklogStatus(raw: String): EpicStatus =
        when {
            raw.contains("TERMIN", ignoreCase = true) ||
                raw.contains("DONE", ignoreCase = true) -> EpicStatus.TERMINE
            raw.contains("EN COURS", ignoreCase = true) ||
                raw.contains("🟡") -> EpicStatus.EN_COURS
            raw.contains("BLOQU", ignoreCase = true) ||
                raw.contains("BLOCKED", ignoreCase = true) -> EpicStatus.BLOQUE
            else -> EpicStatus.PLANIFIE
        }

    private fun extractBoroughFromPath(
        file: Path,
        boroughs: List<BoroughData>,
    ): String? {
        val segments = file.map { it.toString() }.toList().asReversed()
        for (segment in segments) {
            val candidate = segment.lowercase()
            boroughs.firstOrNull { normalizeProject(it.project) == candidate }?.let { return it.name }
            boroughs.firstOrNull { cleanName(it.name).lowercase() == candidate }?.let { return it.name }
        }
        return null
    }

    private fun isAlreadyRepresented(
        root: Path,
        file: Path,
        boroughs: Collection<BoroughData>,
    ): Boolean {
        val segments = runCatching { root.relativize(file) }.getOrNull()?.map { it.toString().lowercase() } ?: return false
        return boroughs.any { borough ->
            val project = normalizeProject(borough.project)
            project.isNotBlank() && segments.contains(project)
        }
    }

    private fun normalizeProject(project: String): String =
        project
            .replace(Regex("\\([^)]*\\)"), "")
            .trim()
            .lowercase()

    /**
     * Some boroughs describe themselves without a `Borough | Project` table
     * (attributes + prose only). Fall back to the repository directory under
     * `foundry/public|private` so that the dashboard still reports them.
     */
    private fun synthesizeBorough(
        root: Path,
        file: Path,
    ): BoroughData? {
        val relative = runCatching { root.relativize(file) }.getOrNull() ?: return null
        val parts = relative.map { it.toString() }
        val projectIndex = if (parts.size >= 2 && parts.first() in REPOSITORY_ROOTS) 1 else 0
        if (projectIndex >= parts.size) return null
        val project = parts[projectIndex]
        if (project.isBlank()) return null

        val content = runCatching { Files.readString(file) }.getOrDefault("")
        val codeName =
            CODE_NAME_ATTRIBUTE
                .find(content)
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        val dagLevel =
            DAG_ATTRIBUTE
                .find(content)
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()
                .orEmpty()

        return BoroughData(
            name = cleanName(codeName ?: prettify(project)),
            project = project,
            dagLevel = dagLevel,
            role = "",
            session = "",
        )
    }

    private fun prettify(project: String): String =
        project
            .removeSuffix("-gradle")
            .split('-')
            .filter { it.isNotBlank() }
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercaseChar() } }

    private fun cleanName(raw: String): String =
        raw
            .replace(Regex("[^\\p{L}\\p{N} .'\\-]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun boroughKey(borough: BoroughData): String = (borough.project.ifBlank { borough.name }).trim().lowercase()

    private fun epicKey(epic: EpicData): String = "${epic.borough.trim().lowercase()}|${epic.id.trim().lowercase()}"

    private fun sessionKey(session: SessionActivity): String = "${session.borough.trim().lowercase()}|${session.number.trim().lowercase()}"

    private fun mergeBorough(
        existing: BoroughData,
        candidate: BoroughData,
    ): BoroughData {
        val winner = if (richness(candidate) > richness(existing)) candidate else existing
        val fallback = if (winner === existing) candidate else existing
        return winner.copy(
            name = winner.name.ifBlank { fallback.name },
            project = winner.project.ifBlank { fallback.project },
            dagLevel = winner.dagLevel.ifBlank { fallback.dagLevel },
            role = winner.role.ifBlank { fallback.role },
            session = winner.session.ifBlank { fallback.session },
        )
    }

    private fun richness(borough: BoroughData): Int = listOf(borough.dagLevel, borough.role, borough.session, borough.project).count { it.isNotBlank() }

    private fun mergeEpic(
        existing: EpicData,
        candidate: EpicData,
    ): EpicData =
        if (candidate.title.isNotBlank() && existing.title.isBlank()) {
            candidate
        } else {
            existing
        }

    private fun mergeSession(
        existing: SessionActivity,
        candidate: SessionActivity,
    ): SessionActivity =
        if (candidate.subject.isNotBlank() && existing.subject.isBlank()) {
            candidate
        } else {
            existing
        }

    private fun parseTables(content: String): List<List<List<String>>> {
        val delimiter = Regex("""^\s*\|={3,}\s*$""")
        val tables = mutableListOf<List<List<String>>>()
        var current: MutableList<String>? = null
        for (line in content.lines()) {
            if (delimiter.matches(line)) {
                if (current == null) {
                    current = mutableListOf()
                } else {
                    val parsed = parseTableBody(current.joinToString("\n"))
                    if (parsed.isNotEmpty()) tables.add(parsed)
                    current = null
                }
            } else {
                current?.add(line)
            }
        }
        // Governance files occasionally leave a table unclosed: parse to EOF.
        current?.let { body ->
            val parsed = parseTableBody(body.joinToString("\n"))
            if (parsed.isNotEmpty()) tables.add(parsed)
        }
        return tables
    }

    /**
     * Parses one AsciiDoc table body.
     *
     * Both single-line rows (`| a | b | c`) and multi-line cells (one `|` line
     * per cell) are supported: cells are accumulated and chunked by the header
     * width. This matters for BACKLOG tables where a row spans several lines.
     */
    private fun parseTableBody(body: String): List<List<String>> {
        val lines = body.lines().map { it.trim() }.filter { it.startsWith("|") }
        if (lines.isEmpty()) return emptyList()
        val width = splitCells(lines.first()).size.coerceAtLeast(1)
        val cells = lines.flatMap { splitCells(it) }
        return cells
            .chunked(width)
            .filter { row -> row.size == width && row.any { cell -> cell.isNotBlank() } }
    }

    private fun splitCells(line: String): List<String> =
        line
            .removePrefix("|")
            .split("|")
            .map { it.trim() }

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
            val status =
                when {
                    role.contains("VESTIGE", ignoreCase = true) -> BoroughStatus.VESTIGE
                    role.contains("DORMANT", ignoreCase = true) -> BoroughStatus.DORMANT
                    else -> BoroughStatus.ACTIVE
                }
            BoroughData(
                name = cleanName(row.getOrElse(boroughIdx) { "" }.replace("==", "")),
                project = project.replace("==", "").trim(),
                dagLevel = dagIdx.takeIf { it >= 0 && it < row.size }?.let { row[it] } ?: "",
                role = role,
                session = sessionIdx.takeIf { it >= 0 && it < row.size }?.let { row[it] } ?: "",
                status = status,
            )
        }
    }

    private fun parseEpicTable(rows: List<List<String>>): List<EpicData>? {
        if (rows.isEmpty()) return null
        val header = rows.first().map { it.lowercase().trim() }
        val epicIdx = header.indexOfFirst { it.contains("epic") }
        val subjectIdx = header.indexOfFirst { it.contains("sujet") || it.contains("subject") }
        val ptsIdx = header.indexOfFirst { it.isPointsHeader() }
        val priorityIdx = header.indexOfFirst { it.contains("priorite") || it.contains("priority") }
        val statusIdx = header.indexOfFirst { it.contains("statut") || it.contains("status") }
        val boroughIdx = header.indexOfFirst { it.contains("borough") }
        if (epicIdx == -1) return null

        return rows.drop(1).map { row ->
            val rawStatus = row.getOrElse(statusIdx.takeIf { it >= 0 && it < row.size } ?: -1) { "" }
            val sessionNumber = extractSessionNumber(rawStatus)
            EpicData(
                id = row.getOrElse(epicIdx) { "" },
                title = subjectIdx.takeIf { it >= 0 && it < row.size }?.let { row[it] } ?: "",
                borough = boroughIdx.takeIf { it >= 0 && it < row.size }?.let { row[it] } ?: "",
                points = ptsIdx.takeIf { it >= 0 && it < row.size }?.let { parsePoints(row[it]) } ?: 0,
                priority = priorityIdx.takeIf { it >= 0 && it < row.size }?.let { row[it] } ?: "P2",
                status = parseEpicStatus(rawStatus),
                session = sessionNumber,
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
                files = filesIdx.takeIf { it >= 0 && it < row.size }?.let { row[it] } ?: "",
            )
        }
    }

    private fun parseEpicStatus(raw: String): EpicStatus =
        when {
            raw.contains("✅") ||
                raw.contains("TERMINE", ignoreCase = true) ||
                raw.contains("DONE", ignoreCase = true) -> EpicStatus.TERMINE
            raw.contains("🔄") ||
                raw.contains("EN COURS", ignoreCase = true) ||
                raw.contains("IN PROGRESS", ignoreCase = true) -> EpicStatus.EN_COURS
            raw.contains("BLOQU", ignoreCase = true) ||
                raw.contains("BLOCKED", ignoreCase = true) -> EpicStatus.BLOQUE
            else -> EpicStatus.PLANIFIE
        }

    private fun extractSessionNumber(raw: String): String {
        val match = Regex("""S?(\d{3})""").find(raw)
        return match?.groupValues?.get(1) ?: ""
    }

    companion object {
        private const val INDEX_FILE = "INDEX.adoc"
        private const val SESSIONS_FILE = "SESSIONS_HISTORY.adoc"
        private const val BACKLOG_FILE = "BACKLOG.adoc"
        private const val TRANSVERSE_BOROUGH = "Transverse"
        private const val TRANSVERSE_PROJECT = "workspace-backlog"

        private val REPOSITORY_ROOTS = setOf("public", "private")
        private val CODE_NAME_ATTRIBUTE = Regex("""^:code-name:\s*(.+)$""", setOf(RegexOption.MULTILINE))
        private val DAG_ATTRIBUTE = Regex("""(?i)_?DAG_?\s*:\s*(N[-\w]+)""")

        private val SKIPPED_DIRECTORIES =
            setOf(
                "build",
                ".gradle",
                ".git",
                ".kotlin",
                ".idea",
                "node_modules",
                "out",
                "target",
                "dist",
                ".venv",
                "venv",
                ".tox",
                "__pycache__",
                ".cache",
                "backup",
                "archives",
            )

        fun skippedDirectories(): Set<String> = SKIPPED_DIRECTORIES
    }
}

/**
 * A points header must name the metric exactly (`Pts`, `Points`), so that
 * loose matches like `Description` (which contains `pt`) never hijack the
 * column and feed dates/prose into the point totals.
 */
private fun String.isPointsHeader(): Boolean {
    val value = trim()
    return value == "pts" || value == "points" || value.startsWith("pts ") || value.startsWith("points ")
}

/**
 * Parses a declared point estimate. Values outside `0..1000` are ignored so a
 * date or an identifier accidentally captured cannot pollute the totals.
 */
private fun parsePoints(raw: String): Int = raw.filter { it.isDigit() }.toIntOrNull()?.takeIf { it in 0..1000 } ?: 0

private fun String.isStatusHeader(): Boolean {
    val value = trim()
    return value.contains("statut") ||
        value.contains("status") ||
        value.contains("session") ||
        value.contains("etat") ||
        value.contains("état")
}
