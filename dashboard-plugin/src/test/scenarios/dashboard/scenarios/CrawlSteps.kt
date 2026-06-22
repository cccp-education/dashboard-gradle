@file:Suppress("unused")

package dashboard.scenarios

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dashboard.model.DashboardData
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.cucumber.java.en.And
import org.assertj.core.api.Assertions.assertThat

class CrawlSteps(private val world: DashboardWorld) {

    @Given("a Gradle project with the dashboard plugin applied")
    fun givenGradleProject() {
        world.createGradleProject()
    }

    @Given("a foundry directory with INDEX.adoc files for {string} and {string}")
    fun givenFoundryDirWithTwoBoroughs(b1: String, b2: String) {
        world.writeIndexAdoc("foundry/${b1.lowercase()}/INDEX.adoc", boroughTable(b1, "${b1.lowercase()}-gradle", "N2"))
        world.writeIndexAdoc("foundry/${b2.lowercase()}/INDEX.adoc", boroughTable(b2, "${b2.lowercase()}-gradle", "N3"))
    }

    @Given("a nonexistent config path {string}")
    fun givenNonexistentConfigPath(path: String) {
        world.setConfigPath(path)
    }

    @Given("a foundry directory with INDEX.adoc containing epics:")
    fun givenFoundryWithEpics(table: DataTable) {
        val rows = table.asMaps()
        val sb = StringBuilder()
        sb.appendLine("= Index — Test")
        sb.appendLine("|===")
        sb.appendLine("| Borough | Project | DAG | Role in MVP0 | Session")
        sb.appendLine("| Test | test-gradle | N0 | Test role | S000")
        sb.appendLine("|===")
        sb.appendLine("|===")
        sb.appendLine("| EPIC | Subject | Pts | Priority | Status")
        for (row in rows) {
            sb.appendLine("| ${row["EPIC"]} | ${row["Subject"]} | ${row["Pts"]} | ${row["Prio"]} | ${row["Status"]}")
        }
        sb.appendLine("|===")
        world.writeIndexAdoc("foundry/test/INDEX.adoc", sb.toString())
    }

    @Given("a foundry directory with INDEX.adoc for borough {string} project {string} dag {string} containing epics:")
    fun givenFoundryWithBoroughEpics(borough: String, project: String, dag: String, table: DataTable) {
        val rows = table.asMaps()
        val sb = StringBuilder()
        sb.appendLine("= Index — $borough")
        sb.appendLine("|===")
        sb.appendLine("| Borough | Project | DAG | Role in MVP0 | Session")
        sb.appendLine("| $borough | $project | $dag | Test role | S000")
        sb.appendLine("|===")
        sb.appendLine("|===")
        sb.appendLine("| EPIC | Subject | Pts | Priority | Status")
        for (row in rows) {
            sb.appendLine("| ${row["EPIC"]} | ${row["Subject"]} | ${row["Pts"]} | ${row["Prio"]} | ${row["Status"]}")
        }
        sb.appendLine("|===")
        world.writeIndexAdoc("foundry/${borough.lowercase()}/INDEX.adoc", sb.toString())
    }

    @Given("a foundry directory with INDEX.adoc and SESSIONS_HISTORY.adoc containing completed epics for {string}")
    fun givenFoundryWithCompletedEpics(borough: String) {
        val lower = borough.lowercase()
        world.writeIndexAdoc(
            "foundry/$lower/INDEX.adoc",
            """
            |===
            | Borough | Project | DAG | Role in MVP0 | Session
            | $borough | $lower-gradle | N3 | Test role | S000
            |===
            |===
            | EPIC | Subject | Pts | Priority | Status
            | DSH-0 | Bootstrap | 3 | P0 | ✅ S000
            |===
            """.trimIndent()
        )
        world.writeIndexAdoc(
            "foundry/$lower/.agents/SESSIONS_HISTORY.adoc",
            """
            = SESSIONS_HISTORY
            |===
            | # | Date | Subject | Files
            | 000 | 2026-06-18 | Bootstrap | 8 files
            |===
            """.trimIndent()
        )
    }

    @Given("a foundry directory with INDEX.adoc and SESSIONS_HISTORY.adoc for {string}")
    fun givenFoundryWithSessions(borough: String) {
        world.writeIndexAdoc(
            "foundry/${borough.lowercase()}/INDEX.adoc",
            """
            |===
            | Borough | Project | DAG | Role in MVP0 | Session
            | $borough | ${borough.lowercase()}-gradle | N3 | Test role | S000
            |===
            """.trimIndent()
        )
        world.writeIndexAdoc(
            "foundry/${borough.lowercase()}/.agents/SESSIONS_HISTORY.adoc",
            """
            = SESSIONS_HISTORY
            |===
            | # | Date | Objet | Fichiers
            | 000 | 2026-06-18 | Bootstrap gouvernance | 8 fichiers
            | 001 | 2026-06-18 | Plugin scaffold | 7 fichiers
            |===
            """.trimIndent()
        )
    }

    @When("I execute the {string} task")
    fun whenExecuteTask(taskName: String) {
        world.executeGradle(taskName)
    }

    @Then("the build should succeed")
    fun thenBuildSucceeds() {
        assertThat(world.buildResult).isNotNull
        assertThat(world.buildResult!!.output).contains("BUILD SUCCESSFUL")
    }

    @Then("the build log should warn {string}")
    fun thenBuildLogWarns(message: String) {
        val output = world.buildResult?.output ?: error("No build result")
        assertThat(output).contains(message)
    }

    @Then("the dashboard data should contain {int} boroughs")
    fun thenDataContainsBoroughs(count: Int) {
        val json = world.readJsonOutput() ?: error("No dashboard JSON output")
        val data = mapper.readValue(json, DashboardData::class.java)
        assertThat(data.boroughs).hasSize(count)
    }

    @Then("the dashboard data should contain borough {string}")
    fun thenDataContainsBorough(name: String) {
        val json = world.readJsonOutput() ?: error("No dashboard JSON output")
        val data = mapper.readValue(json, DashboardData::class.java)
        assertThat(data.boroughs.map { it.name }).contains(name)
    }

    @Then("the dashboard data should contain {int} epics")
    fun thenDataContainsEpics(count: Int) {
        val json = world.readJsonOutput() ?: error("No dashboard JSON output")
        val data = mapper.readValue(json, DashboardData::class.java)
        assertThat(data.epics).hasSize(count)
    }

    @Then("epic {string} should have status {string}")
    fun thenEpicHasStatus(epicId: String, statusRaw: String) {
        val json = world.readJsonOutput() ?: error("No dashboard JSON output")
        val data = mapper.readValue(json, DashboardData::class.java)
        val epic = data.epics.find { it.id == epicId }
            ?: error("Epic $epicId not found")
        assertThat(epic.status.name).isEqualTo(statusRaw)
    }

    @Then("the dashboard data should contain {int} sessions")
    fun thenDataContainsSessions(count: Int) {
        val json = world.readJsonOutput() ?: error("No dashboard JSON output")
        val data = mapper.readValue(json, DashboardData::class.java)
        assertThat(data.sessions).hasSize(count)
    }

    @Then("session {string} should have subject {string}")
    fun thenSessionHasSubject(number: String, subject: String) {
        val json = world.readJsonOutput() ?: error("No dashboard JSON output")
        val data = mapper.readValue(json, DashboardData::class.java)
        val session = data.sessions.find { it.number == number }
            ?: error("Session $number not found")
        assertThat(session.subject).isEqualTo(subject)
    }

    @Then("the dashboard site should contain {string}")
    fun thenDashboardSiteContains(text: String) {
        val html = world.readHtmlOutput() ?: error("No dashboard HTML output")
        assertThat(html).contains(text)
    }

    @Then("the dashboard site should contain a stylesheet link")
    fun thenDashboardSiteContainsStylesheet() {
        val html = world.readHtmlOutput() ?: error("No dashboard HTML output")
        assertThat(html).contains("styles.css")
    }

    @Then("the build log should contain {string}")
    fun thenBuildLogContains(message: String) {
        val output = world.buildResult?.output ?: error("No build result")
        assertThat(output).contains(message)
    }

    @Then("the dashboard site should be published at {string}")
    fun thenDashboardSitePublishedAt(path: String) {
        val published = world.projectDir.resolve(path)
        assertThat(published).exists()
    }

    companion object {
        val mapper = jacksonObjectMapper()
    }

    private fun boroughTable(name: String, project: String, dag: String): String = """
        |===
        | Borough | Project | DAG | Role in MVP0 | Session
        | $name | $project | $dag | Test role | S001
        |===
    """.trimIndent()
}
