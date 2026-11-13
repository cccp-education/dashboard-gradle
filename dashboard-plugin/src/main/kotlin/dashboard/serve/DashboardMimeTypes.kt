package dashboard.serve

/**
 * MIME types used by the local dashboard server.
 *
 * Pure mapping (no I/O) so it is unit-testable and shared by the HTTP handler.
 */
object DashboardMimeTypes {
    fun forPath(path: String): String =
        when (path.substringAfterLast('.', "").lowercase()) {
            "html", "htm" -> "text/html; charset=utf-8"
            "css" -> "text/css; charset=utf-8"
            "js", "mjs" -> "text/javascript; charset=utf-8"
            "json", "map" -> "application/json; charset=utf-8"
            "svg" -> "image/svg+xml"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "ico" -> "image/x-icon"
            "woff" -> "font/woff"
            "woff2" -> "font/woff2"
            "ttf" -> "font/ttf"
            "txt", "adoc" -> "text/plain; charset=utf-8"
            else -> "application/octet-stream"
        }
}
