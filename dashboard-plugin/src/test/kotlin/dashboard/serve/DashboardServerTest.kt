package dashboard.serve

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path

class DashboardServerTest {
    @TempDir
    lateinit var root: Path

    private val client = HttpClient.newHttpClient()

    @Test
    fun `serves index html on the loopback and answers 404 for unknown paths`() {
        Files.writeString(root.resolve("index.html"), "<html>dashboard</html>")
        Files.writeString(root.resolve("styles.css"), "body{color:red}")

        val server = DashboardServer(root, "127.0.0.1", 0).start()
        try {
            assertThat(server.boundPort).isGreaterThan(0)
            val base = "http://127.0.0.1:${server.boundPort}"

            val index = get("$base/")
            assertThat(index.statusCode()).isEqualTo(200)
            assertThat(index.headers().firstValue("content-type").orElse(""))
                .startsWith("text/html")
            assertThat(index.body()).contains("dashboard")

            val css = get("$base/styles.css")
            assertThat(css.statusCode()).isEqualTo(200)
            assertThat(css.headers().firstValue("content-type").orElse("")).startsWith("text/css")

            val missing = get("$base/nope.html")
            assertThat(missing.statusCode()).isEqualTo(404)
        } finally {
            server.stop()
        }
    }

    private fun get(url: String): HttpResponse<String> =
        client.send(
            HttpRequest.newBuilder(URI.create(url)).GET().build(),
            HttpResponse.BodyHandlers.ofString(),
        )
}
