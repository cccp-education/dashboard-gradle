package dashboard.serve

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Minimal local HTTP server for the generated dashboard (DSH-9.1).
 *
 * Design constraints:
 * - JDK only (`com.sun.net.httpserver`), no external dependency.
 * - Binds the requested host (callers use loopback); never exposed publicly.
 * - Serves [root] statically, `/` → `index.html`, 404 otherwise.
 * - Port `0` is accepted (the OS picks a free port) — used by tests.
 */
class DashboardServer(
    private val root: Path,
    private val host: String,
    private val port: Int,
) {
    private var server: HttpServer? = null
    private var executor: ExecutorService? = null

    val boundPort: Int
        get() = server?.address?.port ?: -1

    fun start(): DashboardServer {
        val http = HttpServer.create(InetSocketAddress(host, port), 0)
        val pool =
            Executors.newFixedThreadPool(4) { runnable ->
                Thread(runnable, "dashboard-serve").apply { isDaemon = true }
            }
        http.executor = pool
        http.createContext("/") { exchange ->
            try {
                val file = DashboardStaticContent.resolve(root, exchange.requestURI.path)
                if (file == null) {
                    val body = "Not found".toByteArray(Charsets.UTF_8)
                    exchange.responseHeaders.add("Content-Type", "text/plain; charset=utf-8")
                    exchange.sendResponseHeaders(404, body.size.toLong())
                    exchange.responseBody.use { it.write(body) }
                } else {
                    val body = Files.readAllBytes(file)
                    exchange.responseHeaders.add(
                        "Content-Type",
                        DashboardMimeTypes.forPath(file.fileName.toString()),
                    )
                    exchange.responseHeaders.add("Cache-Control", "no-store")
                    exchange.sendResponseHeaders(200, body.size.toLong())
                    exchange.responseBody.use { it.write(body) }
                }
            } finally {
                exchange.close()
            }
        }
        http.start()
        server = http
        executor = pool
        return this
    }

    fun stop() {
        server?.stop(0)
        server = null
        executor?.shutdownNow()
        executor = null
    }
}
