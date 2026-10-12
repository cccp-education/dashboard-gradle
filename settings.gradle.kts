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
// education.cccp:workspace-catalog:0.0.76 — cross-borough source of truth for plugin versions.
dependencyResolutionManagement {
    versionCatalogs {
        create("ws") {
            from("education.cccp:workspace-catalog:0.0.78")
        }
    }
}

nmcpSettings {
    centralPortal {
        // CI tolerance — empty creds must not block configuration (mirror capsule/others):
        // nmcp only needs real values when actually publishing to Central.
        username = globalProps.getProperty("ossrhUsername") ?: ""
        password = globalProps.getProperty("ossrhPassword") ?: ""
        publishingType = "AUTOMATIC"
    }
}