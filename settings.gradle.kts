pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("com.gradleup.nmcp.settings").version("1.5.0")
}

val globalProps = java.util.Properties().also {
    val globalFile = file(System.getProperty("user.home") + "/.gradle/gradle.properties")
    if (globalFile.exists()) it.load(globalFile.inputStream())
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

rootProject.name = "dashboard-gradle"

include("dashboard-plugin")

// ── MEM-CAT-ROLLOUT-6 — Catalog workspace published (MEMPHIS): single pin per borough (D4) ──
// education.cccp:workspace-catalog:0.0.34 — cross-borough source of truth for plugin versions.
dependencyResolutionManagement {
    versionCatalogs {
        create("ws") {
            from("education.cccp:workspace-catalog:0.0.34")
        }
    }
}

nmcpSettings {
    centralPortal {
        username = globalProps.getProperty("ossrhUsername") ?: error("ossrhUsername not found")
        password = globalProps.getProperty("ossrhPassword") ?: error("ossrhPassword not found")
        publishingType = "AUTOMATIC"
    }
}