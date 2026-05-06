@UI
Feature: Performance API

  Scenario: Check Performance API metrics
    Given page performance validation on direct navigation
    Then validate page performance on reload
    And user navigate to External Cooperation Page through menu for performance validation
    And measure search duration
