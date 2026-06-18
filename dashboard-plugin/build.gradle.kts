plugins {
    signing
    `java-library`
    `maven-publish`
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.publish)
}

group = "education.cccp"
version = "0.0.1"
kotlin.jvmToolchain(24)

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    api(libs.bundles.jackson)
    api(libs.thymeleaf)

    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.slf4j.api)
    testRuntimeOnly(libs.logback.classic)
    testImplementation(libs.assertj)
    testImplementation(gradleTestKit())
    testImplementation(libs.bundles.cucumber)
}

// ── Unit tests — exclude Cucumber scenarios ─────────────────
tasks.named<Test>("test") {
    filter { excludeTestsMatching("education.cccp.dashboard.scenarios.**") }
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
    outputs.cacheIf { true }
}

// ── Cucumber source set config ──────────────────────────────
sourceSets {
    test {
        resources { srcDir("src/test/features") }
        java { srcDir("src/test/scenarios") }
    }
}

// ── Cucumber test task ──────────────────────────────────────
val cucumberTest = tasks.register<Test>("cucumberTest") {
    description = "Runs Cucumber BDD tests."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = configurations.testRuntimeClasspath.get() +
            sourceSets.test.get().output +
            sourceSets.main.get().output
    useJUnitPlatform { excludeEngines("junit-jupiter") }
    systemProperty("cucumber.junit-platform.naming-strategy", "long")
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
    outputs.upToDateWhen { false }
    dependsOn(tasks.classes)
}

tasks.check { dependsOn(cucumberTest) }

gradlePlugin {
    plugins {
        create("dashboard") {
            id = "education.cccp.dashboard"
            implementationClass = "education.cccp.dashboard.DashboardPlugin"
            displayName = "Dashboard Plugin"
            description = "Gradle plugin for workspace vision and progress dashboard."
            tags.set(listOf("dashboard", "workspace", "visualization", "jbake", "thymeleaf"))
        }
    }
    website = "https://github.com/cccp-education/dashboard-gradle"
    vcsUrl = "https://github.com/cccp-education/dashboard-gradle.git"
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        withType<MavenPublication> {
            pom {
                name.set(gradlePlugin.plugins.getByName("dashboard").displayName)
                description.set(gradlePlugin.plugins.getByName("dashboard").description)
                url.set(gradlePlugin.website.get())
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("cccp-education")
                        name.set("CCCP Education")
                        email.set("cccp.edu@gmail.com")
                    }
                }
                scm {
                    connection.set(gradlePlugin.vcsUrl.get())
                    developerConnection.set(gradlePlugin.vcsUrl.get())
                    url.set(gradlePlugin.vcsUrl.get())
                }
                project.findProperty("relocationGroup")?.let { targetGroup ->
                    withXml {
                        val pom = asElement()
                        val doc = pom.ownerDocument
                        val distMgmt = doc.createElement("distributionManagement")
                        val relocation = doc.createElement("relocation")
                        relocation.appendChild(doc.createElement("groupId")).also { it.textContent = targetGroup.toString() }
                        relocation.appendChild(doc.createElement("artifactId")).also { it.textContent = project.name }
                        distMgmt.appendChild(relocation)
                        pom.appendChild(distMgmt)
                    }
                }
            }
        }
    }
    repositories {
        mavenCentral()
    }
}

signing {
    if (System.getenv("CI") != "true" && !version.toString().endsWith("-SNAPSHOT")) {
        sign(publishing.publications)
    }
    useGpgCmd()
}
