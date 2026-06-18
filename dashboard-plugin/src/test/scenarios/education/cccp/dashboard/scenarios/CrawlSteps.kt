@file:Suppress("unused")

package education.cccp.dashboard.scenarios

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import education.cccp.dashboard.model.DashboardData
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
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
        sb.appendLine("| EPIC | Sujet | Pts | Priorite | Statut")
        for (row in rows) {
            sb.appendLine("| ${row["EPIC"]} | ${row["Sujet"]} | ${row["Pts"]} | ${row["Prio"]} | ${row["Statut"]}")
        }
        sb.appendLine("|===")
        world.writeIndexAdoc("foundry/test/INDEX.adoc", sb.toString())
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
