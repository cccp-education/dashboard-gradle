plugins {
    id("education.cccp.dashboard") version "0.0.5"
}

dashboard {
    // DSH-9.2 — crawl the whole workspace (`foundry/`), not the plugin itself.
    // From `dashboard-gradle/`, `../..` is `foundry/`. Override with
    // `-Pdashboard.configPath=<path>` when needed.
    configPath = providers.gradleProperty("dashboard.configPath").getOrElse("../..")
    outputDir = "build/dashboard"
    publishDir = "build/dashboard-publish"
}
