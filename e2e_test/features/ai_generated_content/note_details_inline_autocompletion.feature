@usingMockedOpenAiService
Feature: Details Inline Auto Completion

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topic | details | testingParent |
      | Scrum |         |               |
    When I visit note "Scrum"
    And I type in the details the word "Schroedinger-Team: Scrum " followed by a space

  @ignore
  Scenario: A sentence is suggested
    Given OpenAI will complete the phrase "Schroedinger-Team: Scrum " with "is a popular Software Development Framework."
    Then I see after "Schroedinger-Team: Scrum " the suggestion from AI: "is a popular Software Development Framework."
    When I accept the AI suggestion
    Then I should see "Scrum is a popular Software Development Framework." in the page

  @ignore
  Scenario: A sentence is suggested and declined by the user by continue typing
    When I continue typing "Master "
    Then I see after "Scrum Master " the suggestion from AI: "is a role in the Scrum Framework."

# grayed out suggestion is missing
