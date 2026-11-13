plugins {
    id("education.cccp.dashboard") version "0.0.6"
}

dashboard {
    // DSH-9.2 — crawl the whole workspace (`foundry/`), not the plugin itself.
    // From `dashboard-gradle/`, `../..` is `foundry/`. Override with
    // `-Pdashboard.configPath=<path>` when needed.
    configPath = providers.gradleProperty("dashboard.configPath").getOrElse("../..")
    // DSH-BOUNDARY-2 — workspace-root BACKLOG.adoc (GBL transverse EPICs).
    workspaceBacklog = providers.gradleProperty("dashboard.workspaceBacklog").getOrElse("../../../BACKLOG.adoc")
    outputDir = "build/dashboard"
    publishDir = "build/dashboard-publish"
}
