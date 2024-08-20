import { When, Then } from '@badeball/cypress-cucumber-preprocessor'
import '../support/string_util'
import start from '../start'

When('I have a notebook with the name {string}', (noteTopic: string) => {
  start.routerToNotebooksPage().creatingNotebook(noteTopic)
})

Then(
  'I should see the default expiration of {string} note to be 1 year',
  (noteTopic: string) => {
    start
      .routerToNotebooksPage()
      .assertNoteHasSettingWithValue(noteTopic, 'Certificate Expiry', '1y')
  }
)

When(
  'There is a {string} notebook with assesment that has certification',
  (notebook: string) => {
    start.testability().injectNumbersNotebookWithQuestions(notebook, 2)
    start
      .routerToNotebooksPage()
      .updateAssessmentSettings(notebook, { numberOfQuestion: 2 })
  }
)

When('I Complete an assessment in {string}', (notebook: string) => {
  start
    .navigateToBazaar()
    .selfAssessmentOnNotebook(notebook)
    .answerYesNoQuestionsToScore(3, 3)
})

Then(
  'I should see that the certificate of {string} assesment expires in 1 year from now',
  (notebook: string) => {
    start.assumeAssessmentPage(notebook).expectCerticateHasExprityDate()
  }
)
