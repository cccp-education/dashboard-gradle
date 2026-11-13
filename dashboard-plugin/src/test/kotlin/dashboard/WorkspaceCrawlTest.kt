package dashboard

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

/**
 * DSH-9.2 — non-regression on the real workspace crawl.
 *
 * The dashboard must aggregate the whole `foundry/` tree (30+ boroughs), not the
 * plugin's own governance. Skipped when the sibling boroughs are not checked out
 * (CI clones only this repository).
 */
class WorkspaceCrawlTest {
    @Test
    fun `crawling the real foundry should report the whole workspace`() {
        val foundry = Path.of("../../..").toAbsolutePath().normalize()
        // GBL-013 US-9 — the cross-borough matrix clones only the dashboard
        // borough under foundry/public; the full workspace is proven by the
        // presence of the workspace-bom N0 repository. Skip in the isolated
        // checkout instead of asserting a one-borough crawl.
        assumeTrue(
            Files.isDirectory(foundry.resolve("public/workspace-bom")),
            "full workspace not present (isolated borough checkout): $foundry",
        )

        val data = IndexCrawler().crawlDirectory(foundry)

        assertThat(data.boroughs.size).isGreaterThanOrEqualTo(30)
        assertThat(data.boroughs.map { it.project }).contains(
            "dashboard-gradle",
            "bakery-gradle",
            "codebase-gradle",
            "academy-gradle",
            "video-kernel-gradle",
            "conventions-gradle",
            "workspace-bom",
        )
        assertThat(data.epics.size).isGreaterThanOrEqualTo(300)
        assertThat(data.sessions).isNotEmpty
    }
}
