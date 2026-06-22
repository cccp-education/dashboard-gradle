@dashboard @generate
Feature: Dashboard static site generation

  Scenario: Generate dashboard site from crawled data
    Given a Gradle project with the dashboard plugin applied
    And a foundry directory with INDEX.adoc containing epics:
      | EPIC  | Subject         | Pts | Prio | Status   |
      | DSH-0 | Bootstrap       | 3   | P0   | ✅ S000  |
      | DSH-1 | Plugin scaffold | 8   | P0   | 🔄 S001  |
      | DSH-2 | Crawler         | 13  | P0   | PLANIFIE |
    When I execute the "generateDashboard" task
    Then the build should succeed
    And the dashboard site should contain "EPIC Matrix"
    And the dashboard site should contain "DSH-0"
    And the dashboard site should contain "status-done"
    And the dashboard site should contain a stylesheet link

  Scenario: Generate dashboard site with activity stream
    Given a Gradle project with the dashboard plugin applied
    And a foundry directory with INDEX.adoc and SESSIONS_HISTORY.adoc for "DASHBOARD"
    When I execute the "generateDashboard" task
    Then the build should succeed
    And the dashboard site should contain "Activity Stream"
    And the dashboard site should contain "Bootstrap gouvernance"

  Scenario: Generate dashboard site with epic matrix grouped by borough
    Given a Gradle project with the dashboard plugin applied
    And a foundry directory with INDEX.adoc for borough "BAKERY" project "bakery-gradle" dag "N2" containing epics:
      | EPIC  | Subject     | Pts | Prio | Status   |
      | BKY-1 | Bakery task | 5   | P1   | EN COURS |
    And a foundry directory with INDEX.adoc for borough "DASHBOARD" project "dashboard-gradle" dag "N3" containing epics:
      | EPIC  | Subject   | Pts | Prio | Status  |
      | DSH-0 | Bootstrap | 3   | P0   | TERMINE |
    When I execute the "generateDashboard" task
    Then the build should succeed
    And the dashboard site should contain "BAKERY (N2)"
    And the dashboard site should contain "DASHBOARD (N3)"
    And the dashboard site should contain "BKY-1"
    And the dashboard site should contain "DSH-0"

  Scenario: Generate dashboard site with timeline
    Given a Gradle project with the dashboard plugin applied
    And a foundry directory with INDEX.adoc and SESSIONS_HISTORY.adoc containing completed epics for "DASHBOARD"
    When I execute the "generateDashboard" task
    Then the build should succeed
    And the dashboard site should contain "Timeline"
    And the dashboard site should contain "Bootstrap"
    And the dashboard site should contain "2026-06-18"

  Scenario: Publish dashboard site copies generated output to publish directory
    Given a Gradle project with the dashboard plugin applied
    And a foundry directory with INDEX.adoc containing epics:
      | EPIC  | Subject   | Pts | Prio | Status  |
      | DSH-0 | Bootstrap | 3   | P0   | TERMINE |
    When I execute the "publishDashboard" task
    Then the build should succeed
    And the build log should contain "Dashboard published"
    And the dashboard site should be published at "build/dashboard-publish/index.html"
