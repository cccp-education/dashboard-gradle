pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }
}
// CNV-16 — JitPack mirror for the education.cccp.build.* conventions
// (bypasses Sonatype / Gradle Plugin Portal). The JitPack tag is pinned while
// the canonical plugin declarations may stay on the released version.
pluginManagement.repositories.maven("https://jitpack.io")
pluginManagement.resolutionStrategy.eachPlugin {
    if (requested.id.id.startsWith("education.cccp.build.")) {
        useModule("com.github.cccp-education.conventions-gradle:conventions-plugin:v0.0.17")
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
// Catalog workspace served by JitPack (CNV-17) — single pin per borough (D4)
dependencyResolutionManagement {
    versionCatalogs {
        create("ws") {
            from("com.github.cccp-education.workspace-bom:workspace-catalog:0.0.136")
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
dependencyResolutionManagement.repositories.maven("https://jitpack.io")
