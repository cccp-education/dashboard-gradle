@dashboard @crawl
Feature: Dashboard data crawling

  Scenario: Crawl INDEX.adoc files in a foundry directory
    Given a Gradle project with the dashboard plugin applied
    And a foundry directory with INDEX.adoc files for "BAKERY" and "NEWARK"
    When I execute the "crawlDashboard" task
    Then the build should succeed
    And the dashboard data should contain 2 boroughs
    And the dashboard data should contain borough "BAKERY"
    And the dashboard data should contain borough "NEWARK"

  Scenario: Crawl with missing config directory
    Given a Gradle project with the dashboard plugin applied
    And a nonexistent config path "invalid-path"
    When I execute the "crawlDashboard" task
    Then the build should succeed
    And the build log should warn "Config path does not exist"

  Scenario: Crawl INDEX.adoc with epic table
    Given a Gradle project with the dashboard plugin applied
    And a foundry directory with INDEX.adoc containing epics:
      | EPIC    | Sujet                | Pts | Prio | Statut   |
      | DSH-0   | Bootstrap            | 3   | P0   | ✅ S000  |
      | DSH-1   | Plugin scaffold      | 8   | P0   | 🔄 S001  |
      | DSH-2   | Crawler              | 13  | P0   | PLANIFIE |
    When I execute the "crawlDashboard" task
    Then the build should succeed
    And the dashboard data should contain 3 epics
    And epic "DSH-0" should have status "TERMINE"
    And epic "DSH-1" should have status "EN_COURS"
    And epic "DSH-2" should have status "PLANIFIE"

  Scenario: Crawl SESSIONS_HISTORY.adoc for activity stream
    Given a Gradle project with the dashboard plugin applied
    And a foundry directory with INDEX.adoc and SESSIONS_HISTORY.adoc for "DASHBOARD"
    When I execute the "crawlDashboard" task
    Then the build should succeed
    And the dashboard data should contain 2 sessions
    And session "000" should have subject "Bootstrap gouvernance"
