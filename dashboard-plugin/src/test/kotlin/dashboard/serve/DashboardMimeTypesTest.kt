package dashboard.serve

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DashboardMimeTypesTest {
    @Test
    fun `html css and javascript are served with utf-8 text types`() {
        assertThat(DashboardMimeTypes.forPath("index.html")).isEqualTo("text/html; charset=utf-8")
        assertThat(DashboardMimeTypes.forPath("styles.css")).isEqualTo("text/css; charset=utf-8")
        assertThat(DashboardMimeTypes.forPath("app.js")).isEqualTo("text/javascript; charset=utf-8")
    }

    @Test
    fun `json and assets keep their specific types`() {
        assertThat(DashboardMimeTypes.forPath("dashboard-data.json"))
            .isEqualTo("application/json; charset=utf-8")
        assertThat(DashboardMimeTypes.forPath("logo.svg")).isEqualTo("image/svg+xml")
        assertThat(DashboardMimeTypes.forPath("favicon.ico")).isEqualTo("image/x-icon")
    }

    @Test
    fun `unknown and extensionless paths fall back to octet-stream`() {
        assertThat(DashboardMimeTypes.forPath("archive.bin")).isEqualTo("application/octet-stream")
        assertThat(DashboardMimeTypes.forPath("README")).isEqualTo("application/octet-stream")
    }
}
