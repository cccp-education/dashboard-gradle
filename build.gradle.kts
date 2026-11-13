import dashboard.CriticalPathRing

plugins {
    id("education.cccp.dashboard") version "0.0.7"
}

dashboard {
    // DSH-9.2 — crawl the whole workspace (`foundry/`), not the plugin itself.
    // From `dashboard-gradle/`, `../..` is `foundry/`. Override with
    // `-Pdashboard.configPath=<path>` when needed.
    configPath = providers.gradleProperty("dashboard.configPath").getOrElse("../..")
    // DSH-BOUNDARY-2 — workspace-root BACKLOG.adoc (GBL transverse EPICs).
    workspaceBacklog = providers.gradleProperty("dashboard.workspaceBacklog").getOrElse("../../../BACKLOG.adoc")
    // DSH-BOUNDARY-4 — MVP0 critical path (vision zone), owned by the workspace.
    criticalPath.set(
        listOf(
            CriticalPathRing("A1", "Corpus métier", emptyList(), "blocked"),
            CriticalPathRing("A2", "Génération pédagogique", listOf("training-gradle", "planner-gradle"), "blocked"),
            CriticalPathRing(
                "A3",
                "Consommateurs N2",
                listOf("slider-gradle", "document-gradle", "capsule-gradle", "quizz-benchmark-gradle"),
                "ready",
            ),
            CriticalPathRing(
                "A4",
                "Site + commerce",
                listOf("bakery-gradle", "runner-gradle", "waiter-gradle"),
                "ready",
            ),
        ),
    )
    outputDir = "build/dashboard"
    publishDir = "build/dashboard-publish"
}
