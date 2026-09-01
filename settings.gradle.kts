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

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

rootProject.name = "dashboard-gradle"

include("dashboard-plugin")

nmcpSettings {
    centralPortal {
        publishingType = "AUTOMATIC"
    }
}