package dashboard.serve

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class DashboardStaticContentTest {
    @TempDir
    lateinit var root: Path

    @BeforeEach
    fun setUp() {
        Files.writeString(root.resolve("index.html"), "<html>home</html>")
        Files.writeString(root.resolve("styles.css"), "body{}")
        Files.createDirectories(root.resolve("sub"))
        Files.writeString(root.resolve("sub/index.html"), "<html>sub</html>")
    }

    @Test
    fun `root and directory resolve to index html`() {
        assertThat(DashboardStaticContent.resolve(root, "/")).isEqualTo(root.resolve("index.html"))
        assertThat(DashboardStaticContent.resolve(root, "/sub")).isEqualTo(root.resolve("sub/index.html"))
    }

    @Test
    fun `a regular file is served by its relative path`() {
        assertThat(DashboardStaticContent.resolve(root, "/styles.css")).isEqualTo(root.resolve("styles.css"))
    }

    @Test
    fun `query strings are ignored`() {
        assertThat(DashboardStaticContent.resolve(root, "/styles.css?v=42")).isEqualTo(root.resolve("styles.css"))
    }

    @Test
    fun `missing files resolve to null`() {
        assertThat(DashboardStaticContent.resolve(root, "/missing.html")).isNull()
    }

    @Test
    fun `path traversal is refused`() {
        Files.writeString(root.parent.resolve("secret.txt"), "do not serve")
        assertThat(DashboardStaticContent.resolve(root, "/../secret.txt")).isNull()
        assertThat(DashboardStaticContent.resolve(root, "/%2e%2e/secret.txt")).isNull()
    }
}
