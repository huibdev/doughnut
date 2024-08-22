/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check
import {
  DataTable,
  Given,
  Then,
  When,
} from '@badeball/cypress-cucumber-preprocessor'
import start, { mock_services } from '../start'

Given(
  "my question should not be included in the admin's fine-tuning data",
  () => {
    start
      .loginAsAdminAndGoToAdminDashboard()
      .goToFineTuningData()
      .expectFineTuningExamplesCount(0)
  }
)

Given(
  'the admin modifies the question suggested {string} to:',
  (originalQuestionStem: string, newQuestion: DataTable) => {
    start
      .loginAsAdminAndGoToAdminDashboard()
      .goToFineTuningData()
      .updateQuestionSuggestionAndChoice(
        originalQuestionStem,
        newQuestion.hashes()[0] as Record<string, string>
      )
  }
)

Given('an admin duplicates the question {string}', (questionStem: string) => {
  start
    .loginAsAdminAndGoToAdminDashboard()
    .goToFineTuningData()
    .duplicateNegativeQuestion(questionStem)
})

Given(
  'an admin can retrieve the training data for question generation containing:',
  (question: DataTable) => {
    start
      .loginAsAdminAndGoToAdminDashboard()
      .goToFineTuningData()
      .expectExampleQuestions(question.hashes())
  }
)

Given(
  'an admin can retrieve the training data for question generation containing {int} examples',
  (numberOfRecords: number) => {
    start
      .loginAsAdminAndGoToAdminDashboard()
      .goToFineTuningData()
      .expectFineTuningExamplesCount(numberOfRecords)
  }
)

Given(
  'there should be {int} examples containing {string}',
  (numOfOccurrence: number, expectedString: string) => {
    start
      .assumeAdminDashboardPage()
      .goToFineTuningData()
      .expectString(numOfOccurrence, expectedString)
  }
)

Given('I am logged in as an admin', (_tabName: string) => {
  start.loginAsAdmin()
})

Given(
  'I navigate to the {string} section in the admin dashboard',
  (tabName: string) => {
    start.goToAdminDashboard().goToTabInAdminDashboard(tabName)
  }
)

Given('OpenAI responds with {string} when uploading file', (result) => {
  mock_services.openAi().stubOpenAiUploadResponse(result === 'success')
})

Given('OpenAI responds with {string} when triggering fine-tuning', (result) => {
  mock_services.openAi().stubFineTuningStatus(result === 'success')
})

When('I attempt to trigger fine-tuning', () => {
  start
    .loginAsAdminAndGoToAdminDashboard()
    .goToFineTuningData()
    .triggerFineTuning()
})

Then('I should see the message {string}', (message: string) => {
  cy.contains(message)
})

Given(
  'I have {int} positive feedbacks and {int} negative feedbacks',
  (positive: number, negative: number) => {
    const positives = Array.from({ length: positive }, (_, index) => ({
      positiveFeedback: true,
      preservedNoteContent: 'note content',
      realCorrectAnswers: '',
      preservedQuestion: {
        multipleChoicesQuestion: {
          stem: `good question #${index}`,
          choices: ['choice 1', 'choice 2'],
        },
        correctChoiceIndex: 0,
      },
    }))
    const negatives = Array.from({ length: negative }, (_, index) => ({
      positiveFeedback: false,
      preservedNoteContent: 'note content',
      realCorrectAnswers: '',
      preservedQuestion: {
        multipleChoicesQuestion: {
          stem: `bad question #${index}`,
          choices: ['choice 1', 'choice 2'],
        },
        correctChoiceIndex: 0,
      },
    }))

    start.testability().injectSuggestedQuestions(positives.concat(negatives))
  }
)

Then('I choose model {string} for {string}', (model: string, task: string) => {
  start.goToAdminDashboard().goToModelManagement().chooseModel(model, task)
})

Given(
  'OpenAI creates an assistant of ID {string} for name {string} with model {string}',
  (newId: string, nameOfAssistant: string, modelName: string) => {
    mock_services
      .openAi()
      .stubCreateAssistant(newId, nameOfAssistant, modelName)
  }
)

Given(
  'OpenAI creates an assistant of ID {string} for name {string} with additional instruction {string}',
  (newId: string, nameOfAssistant: string, additionalInstruction: string) => {
    mock_services
      .openAi()
      .stubCreateAssistant(
        newId,
        nameOfAssistant,
        'gpt-3.5-turbo',
        additionalInstruction
      )
  }
)

When(
  'I recreate all the assitants and the new assistant ID should be {string} for {string}',
  (newId: string, nameOfAssistant: string) => {
    start
      .goToAdminDashboard()
      .goToAssistantManagement()
      .recreate()
      .expectNewAssistant(newId, nameOfAssistant)
  }
)

When('I remove the notebook {string} from the bazaar', (notebook: string) => {
  start.goToAdminDashboard().goToBazaarManagement().removeFromBazaar(notebook)
})

When('I open certification approval page', () => {
  start.goToAdminDashboard().goToCertificationRequestPage()
})

Then('I should not see any pending approval requests', () => {
  start.goToAdminDashboard().goToCertificationRequestPage().listIsEmpty()
})

When('I approve notebook {string}', (notebook: string) => {
  start.goToAdminDashboard().goToCertificationRequestPage().approve(notebook)
})

Then(
  'I should not see notebook {string} waiting for approval',
  (notebook: string) => {
    start
      .goToAdminDashboard()
      .goToCertificationRequestPage()
      .listDoesNotContain(notebook)
  }
)

Then(
  'I should see following notebooks waiting for approval:',
  (notebooks: DataTable) => {
    notebooks.raw().forEach((notebookRaw: string[]) => {
      start
        .goToAdminDashboard()
        .goToCertificationRequestPage()
        .listContains(notebookRaw[0]!)
    })
  }
)
