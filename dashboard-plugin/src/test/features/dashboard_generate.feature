@dashboard @generate
Feature: Dashboard static site generation

  Scenario: Generate dashboard site from crawled data
    Given a Gradle project with the dashboard plugin applied
    And a foundry directory with INDEX.adoc containing epics:
      | EPIC  | Sujet           | Pts | Prio | Statut   |
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
