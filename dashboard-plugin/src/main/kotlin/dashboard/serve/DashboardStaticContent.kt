package dashboard.serve

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/**
 * Resolves a request path to a file inside the generated dashboard directory.
 *
 * Security: the resolved path is normalised and must stay under [root]
 * (path-traversal guard). `/` and directories resolve to `index.html`.
 * Pure domain (no HTTP, no server) so the traversal rule is unit-tested.
 */
object DashboardStaticContent {
    fun resolve(
        root: Path,
        requestPath: String,
    ): Path? {
        val decoded =
            runCatching {
                URLDecoder.decode(requestPath.substringBefore('?'), StandardCharsets.UTF_8)
            }.getOrNull() ?: return null

        val normalizedRoot = root.toAbsolutePath().normalize()
        val relative = decoded.trimStart('/').ifEmpty { "index.html" }
        val candidate = normalizedRoot.resolve(relative).normalize()
        if (!candidate.startsWith(normalizedRoot)) return null

        val file = if (Files.isDirectory(candidate)) candidate.resolve("index.html") else candidate
        return file.takeIf { Files.isRegularFile(it) }
    }
}
