Feature: Self assessment
  As a trainer, I want to create a notebook with knowledge and questions
  and share it in the Bazaar, so that people can use it to assess their own skill level and knowledge on the topic

  Background:
    Given I am logged in as an existing user
    And I have a notebook with head note "Countries" and notes:
      | Topic     | Parent Topic |
      | Singapore | Countries    |
      | Vietnam   | Countries    |
      | Japan     | Countries    |
    And notebook "Countries" is shared to the Bazaar
    And there are questions in the notebook "Countries" for the note:
      | Note Topic | Question                           | Answer | One Wrong Choice | Approved |
      | Singapore  | Where in the world is Singapore?   | Asia   | europe           | true     |
      | Vietnam    | Most famous food of Vietnam?       | Pho    | bread            | true     |
      | Japan      | What is the capital city of Japan? | Tokyo  | kyoto            | true     |

  Scenario Outline: Perform an assessment with variable outcomes counts correct scores
    Given I set the number of questions per assessment of the notebook "Countries" to 3
    When I do the assessment on "Countries" in the bazaar with the following answers:
      | Question                           | Answer            |
      | Where in the world is Singapore?   | <SingaporeAnswer> |
      | Most famous food of Vietnam?       | <VietnamAnswer>   |
      | What is the capital city of Japan? | <JapanAnswer>     |
    Then I should see the score "Your score: <ExpectedScore> / 3" at the end of assessment

    Examples:
      | SingaporeAnswer | VietnamAnswer | JapanAnswer | ExpectedScore |
      | Asia            | Pho           | Tokyo       | 3             |
      | europe          | bread         | kyoto       | 0             |
      | Asia            | Pho           | kyoto       | 2             |

  Scenario Outline: Cannot start assessment with 0 questions or not enough approved questions
    Given I set the number of questions per assessment of the notebook "Countries" to <Questions Per Assessment>
    When I start the assessment on the "Countries" notebook in the bazaar
    Then I should see error message <Message>

    Examples:
      | Questions Per Assessment | Message                         |
      | 0                        | The assessment is not available |
      | 10                       | Not enough questions            |

  Scenario: Must login to generate assessment
    Given I haven't login
    When I start the assessment on the "Countries" notebook in the bazaar
    Then I should see message that says "Please login first"

  @ignore # this is a flaky test that we are trying to figure out why it is flaky
  Scenario: One user cannot perform an assessment more than 3 times per day
    Given the number of questions in assessment for notebook "Countries" is 1
    When I have done the assessment of the notebook "Countries" 3 times
    Then I should not be able to do assessment of the notebook "Countries" any more today
    And I should be able to do assessment of the notebook "Countries" again the next day

