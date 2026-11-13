pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}
// CNV-16 — JitPack mirror for the education.cccp.build.* conventions
// (bypasses Sonatype / Gradle Plugin Portal). The JitPack tag is pinned while
// the canonical plugin declarations may stay on the released version.
pluginManagement.repositories.maven("https://jitpack.io")
pluginManagement.resolutionStrategy.eachPlugin {
    if (requested.id.id == "education.cccp.dashboard") {
        // CNV-17 — the plugin marker is not served at the plugin-id group on
        // JitPack; resolve the module explicitly (same shim as other boroughs).
        // Local development: `publishToMavenLocal` publishes
        // `education.cccp:dashboard-plugin:<v>`; prefer it when present so the
        // root consumer works before the tag is pushed. Force JitPack with
        // `-PdashboardUseJitPack=true`.
        val version = requested.version
        val localJar =
            java.io.File(
                System.getProperty("user.home"),
                ".m2/repository/education/cccp/dashboard-plugin/$version/dashboard-plugin-$version.jar",
            )
        val forceJitPack = providers.gradleProperty("dashboardUseJitPack").orNull == "true"
        if (!forceJitPack && localJar.isFile) {
            useModule("education.cccp:dashboard-plugin:$version")
        } else {
            useModule("com.github.cccp-education.dashboard-gradle:dashboard-plugin:$version")
        }
    }
}
pluginManagement.resolutionStrategy.eachPlugin {
    if (requested.id.id.startsWith("education.cccp.build.")) {
        useModule("com.github.cccp-education.conventions-gradle:conventions-plugin:0.0.17")
    }
}

dependencyResolutionManagement.repositories.maven("https://jitpack.io")
rootProject.name = "dashboard-gradle"