/// <reference types="Cypress" />
// @ts-check
import { Randomization } from './../../frontend/src/generated/backend/models/Randomization'
import { QuestionSuggestionParams } from '../../frontend/src/generated/backend/models/QuestionSuggestionParams'
import ServiceMocker from '../support/ServiceMocker'
import { NoteTestData } from './../../frontend/src/generated/backend/models/NoteTestData'
import { QuizQuestionsTestData } from './../../frontend/src/generated/backend/models/QuizQuestionsTestData'

const hourOfDay = (days: number, hours: number) => {
  return new Date(1976, 5, 1 + days, hours)
}

const postToTestabilityApi = (
  cy: Cypress.cy & CyEventEmitter,
  path: string,
  options: { body?: Record<string, unknown>; failOnStatusCode?: boolean }
) => {
  return cy.request({
    method: 'POST',
    url: `/api/testability/${path}`,
    ...options,
  })
}

const postToTestabilityApiSuccessfully = (
  cy: Cypress.cy & CyEventEmitter,
  path: string,
  options: { body?: Record<string, unknown>; failOnStatusCode?: boolean }
) => {
  return postToTestabilityApi(cy, path, options)
    .its('status')
    .should('equal', 200)
}

const cleanAndReset = (cy: Cypress.cy & CyEventEmitter, countdown: number) => {
  postToTestabilityApi(cy, 'clean_db_and_reset_testability_settings', {
    failOnStatusCode: countdown === 1,
  }).then((response) => {
    if (countdown > 0 && response.status !== 200) {
      cleanAndReset(cy, countdown - 1)
    }
  })
}

const injectedNoteIdMapAliasName = 'injectedNoteIdMap'

const testability = () => {
  return {
    cleanDBAndResetTestabilitySettings() {
      cleanAndReset(cy, 5)
      cy.wrap({}).as(injectedNoteIdMapAliasName)
    },

    featureToggle(enabled: boolean) {
      postToTestabilityApiSuccessfully(cy, 'feature_toggle', {
        body: { enabled },
      })
    },

    injectNotes(
      noteTestData: NoteTestData[],
      externalIdentifier = '',
      circleName: string | null = null
    ) {
      postToTestabilityApi(cy, 'inject_notes', {
        body: {
          externalIdentifier,
          circleName,
          noteTestData,
        },
      }).then((response) => {
        expect(Object.keys(response.body).length).to.equal(noteTestData.length)
        cy.get(`@${injectedNoteIdMapAliasName}`).then((existingMap) => {
          const mergedMap = { ...existingMap, ...response.body }
          cy.wrap(mergedMap).as(injectedNoteIdMapAliasName)
        })
      })
    },
    injectNumberNotes(notebook: string, numberOfNotes: number) {
      const notes: Record<string, string>[] = [
        { Topic: notebook },
        ...new Array(numberOfNotes).fill(0).map((_, index) => ({
          Topic: `Note about ${index}`,
          'Parent Topic': notebook,
        })),
      ]
      return this.injectNotes(notes)
    },
    injectQuizQuestions(quizQuestionsTestData: QuizQuestionsTestData) {
      postToTestabilityApi(cy, 'inject_quiz_questions', {
        body: quizQuestionsTestData,
      }).then((response) => {
        expect(Object.keys(response.body).length).to.equal(
          quizQuestionsTestData.quizQuestionTestData?.length
        )
      })
    },
    injectYesNoQuestionsForNumberNotes(
      notebook: string,
      numberOfNotes: number
    ) {
      const quizQuestion: Record<string, string>[] = new Array(numberOfNotes)
        .fill(0)
        .map((_, index) => ({
          'Note Topic': `Note about ${index}`,
          Question: `Is ${index} * ${index} = ${index * index}?`,
          Answer: 'Yes',
          'One Wrong Choice': 'No',
          Approved: 'true',
        }))
      return this.injectQuizQuestions({
        notebookTitle: notebook,
        quizQuestionTestData: quizQuestion,
      })
    },
    injectNumbersNotebookWithQuestions(
      notebook: string,
      numberOfQuestion: number
    ) {
      this.injectNumberNotes(notebook, numberOfQuestion)
      this.injectYesNoQuestionsForNumberNotes(notebook, numberOfQuestion)
      this.shareToBazaar(notebook)
    },
    injectLink(type: string, fromNoteTopic: string, toNoteTopic: string) {
      cy.get(`@${injectedNoteIdMapAliasName}`).then((injectedNoteIdMap) => {
        expect(injectedNoteIdMap).haveOwnPropertyDescriptor(fromNoteTopic)
        expect(injectedNoteIdMap).haveOwnPropertyDescriptor(toNoteTopic)
        const fromNoteId = injectedNoteIdMap[fromNoteTopic]
        const toNoteId = injectedNoteIdMap[toNoteTopic]
        postToTestabilityApiSuccessfully(cy, 'link_notes', {
          body: {
            type,
            source_id: fromNoteId,
            target_id: toNoteId,
          },
        })
      })
    },

    injectSuggestedQuestions(examples: Array<QuestionSuggestionParams>) {
      postToTestabilityApiSuccessfully(cy, 'inject_suggested_questions', {
        body: {
          examples,
        },
      })
    },

    injectSuggestedQuestion(questionStem: string, positiveFeedback: boolean) {
      this.injectSuggestedQuestions([
        {
          positiveFeedback,
          preservedNoteContent: 'note content',
          realCorrectAnswers: '',
          preservedQuestion: {
            multipleChoicesQuestion: {
              stem: questionStem,
              choices: ['choice 1', 'choice 2'],
            },
            correctChoiceIndex: 0,
          },
        },
      ])
    },

    getInjectedNoteIdByTitle(noteTopic: string) {
      return cy
        .get(`@${injectedNoteIdMapAliasName}`)
        .then((injectedNoteIdMap) => {
          expect(
            injectedNoteIdMap,
            `"${noteTopic}" is not in the injected note. Did you created during the test?`
          ).haveOwnPropertyDescriptor(noteTopic)
          return injectedNoteIdMap[noteTopic]
        })
    },

    timeTravelTo(day: number, hour: number) {
      this.backendTimeTravelTo(day, hour)
      cy.window().then((window) => {
        cy.tick(hourOfDay(day, hour).getTime() - new window.Date().getTime())
      })
    },

    backendTimeTravelToDate(date: Date) {
      postToTestabilityApiSuccessfully(cy, 'time_travel', {
        body: { travel_to: JSON.stringify(date) },
      })
    },

    backendTimeTravelTo(day: number, hour: number) {
      postToTestabilityApiSuccessfully(cy, 'time_travel', {
        body: { travel_to: JSON.stringify(hourOfDay(day, hour)) },
      })
    },

    backendTimeTravelRelativeToNow(hours: number) {
      return postToTestabilityApiSuccessfully(
        cy,
        'time_travel_relative_to_now',
        {
          body: { hours: JSON.stringify(hours) },
        }
      )
    },

    randomizerSettings(strategy: 'first' | 'last' | 'seed', seed: number) {
      postToTestabilityApiSuccessfully(cy, 'randomizer', {
        body: <Randomization>{ choose: strategy, seed },
      })
    },

    triggerException() {
      postToTestabilityApi(cy, 'trigger_exception', { failOnStatusCode: false })
    },

    shareToBazaar(noteTopic: string) {
      postToTestabilityApiSuccessfully(cy, 'share_to_bazaar', {
        body: { noteTopic },
      })
    },

    injectCircle(circleInfo: Record<string, string>) {
      postToTestabilityApiSuccessfully(cy, 'inject_circle', {
        body: circleInfo,
      })
    },

    updateCurrentUserSettingsWith(hash: Record<string, string>) {
      postToTestabilityApiSuccessfully(cy, 'update_current_user', {
        body: hash,
      })
    },

    setServiceUrl(serviceName: string, serviceUrl: string) {
      return postToTestabilityApi(cy, `replace_service_url`, {
        body: { [serviceName]: serviceUrl },
      }).then((response) => {
        expect(response.body).to.haveOwnProperty(serviceName)
        expect(response.body[serviceName]).to.include('http')
        cy.wrap(response.body[serviceName])
      })
    },
    mockBrowserTime() {
      //
      // when using `cy.clock()` to set the time,
      // for Vue component with v-if for a ref/react object that is changed during mount by async call
      // the event, eg. click, will not work.
      //
      cy.clock(hourOfDay(0, 0), [
        'setTimeout',
        'setInterval',
        'clearInterval',
        'clearTimeout',
        'Date',
      ])
    },
    mockService(serviceMocker: ServiceMocker) {
      this.setServiceUrl(
        serviceMocker.serviceName,
        serviceMocker.serviceUrl
      ).as(serviceMocker.savedServiceUrlName)
      serviceMocker.install()
    },

    restoreMockedService(serviceMocker: ServiceMocker) {
      cy.get(`@${serviceMocker.savedServiceUrlName}`).then((saved) =>
        this.setServiceUrl(
          serviceMocker.serviceName,
          saved as unknown as string
        )
      )
    },
  }
}

export default testability
