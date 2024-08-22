package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import com.odde.doughnut.testability.builders.QuizQuestionBuilder;
import java.time.Period;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RestNotebookControllerTest {
  @Autowired ModelFactoryService modelFactoryService;

  @Autowired MakeMe makeMe;
  private UserModel userModel;
  private Note topNote;
  RestNotebookController controller;
  private TestabilitySettings testabilitySettings = new TestabilitySettings();

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    topNote = makeMe.aNote().creatorAndOwner(userModel).please();
    controller = new RestNotebookController(modelFactoryService, userModel, testabilitySettings);
  }

  @Nested
  class showNoteTest {
    @Test
    void whenNotLogin() {
      userModel = modelFactoryService.toUserModel(null);
      controller = new RestNotebookController(modelFactoryService, userModel, testabilitySettings);
      assertThrows(ResponseStatusException.class, () -> controller.myNotebooks());
    }

    @Test
    void whenLoggedIn() {
      User user = new User();
      userModel = modelFactoryService.toUserModel(user);
      List<Notebook> notebooks = userModel.getEntity().getOwnership().getNotebooks();
      controller = new RestNotebookController(modelFactoryService, userModel, testabilitySettings);
      assertEquals(notebooks, controller.myNotebooks().notebooks);
    }
  }

  @Nested
  class ShareMyNotebook {

    @Test
    void shareMyNote() throws UnexpectedNoAccessRightException {
      long oldCount = modelFactoryService.bazaarNotebookRepository.count();
      controller.shareNotebook(topNote.getNotebook());
      assertThat(modelFactoryService.bazaarNotebookRepository.count(), equalTo(oldCount + 1));
    }

    @Test
    void shouldNotBeAbleToShareNoteThatBelongsToOtherUser() {
      User anotherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().creatorAndOwner(anotherUser).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.shareNotebook(note.getNotebook()));
    }
  }

  @Nested
  class updateNotebook {
    @Test
    void shouldNotBeAbleToUpdateNotebookThatBelongsToOtherUser() {
      User anotherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().creatorAndOwner(anotherUser).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.update(note.getNotebook(), new NotebookSettings()));
    }

    @Test
    void shouldBeAbleToEditCertificateExpiry() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().creatorAndOwner(userModel).please();
      var notebookSettings = new NotebookSettings();
      notebookSettings.setCertificateExpiry(Period.parse("P2Y3M"));
      controller.update(note.getNotebook(), notebookSettings);
      assertThat(
          note.getNotebook().getNotebookSettings().getCertificateExpiry(),
          equalTo(Period.parse("P2Y3M")));
    }
  }

  @Nested
  class requestNotebookApproval {
    @Test
    void shouldNotBeAbleToRequestApprovalForNotebookThatBelongsToOtherUser() {
      User anotherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().creatorAndOwner(anotherUser).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.requestNotebookApproval(note.getNotebook()));
    }

    @Test
    void approvalStatusShouldBePendingAfterRequestingApproval()
        throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().creatorAndOwner(userModel).please();
      controller.requestNotebookApproval(note.getNotebook());
      assertThat(note.getNotebook().getApprovalStatus(), equalTo(ApprovalStatus.PENDING));
    }
  }

  @Nested
  class DownloadNotebookDump {
    private Notebook notebook;

    @BeforeEach
    void setup() {
      notebook = makeMe.aNote().creatorAndOwner(userModel).please().getNotebook();
      makeMe.refresh(notebook);
    }

    @Test
    void whenNotAuthorized() {
      User anotherUser = makeMe.aUser().please();
      controller =
          new RestNotebookController(
              modelFactoryService,
              modelFactoryService.toUserModel(anotherUser),
              testabilitySettings);
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.downloadNotebookDump(notebook));
    }

    @Test
    void whenAuthorized() throws UnexpectedNoAccessRightException {
      List<Note.NoteBrief> noteBriefs = controller.downloadNotebookDump(notebook);
      assertThat(noteBriefs, hasSize(1));
    }
  }

  @Nested
  class MoveToCircle {
    @Test
    void shouldNotBeAbleToMoveNotebookThatIsCreatedByAnotherUser() {
      User anotherUser = makeMe.aUser().please();
      Circle circle1 = makeMe.aCircle().hasMember(anotherUser).hasMember(userModel).please();
      Note note = makeMe.aNote().creator(anotherUser).inCircle(circle1).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.moveToCircle(note.getNotebook(), makeMe.aCircle().please()));
    }
  }

  @Nested
  class GetNotebookQuestions {
    Notebook notebook;
    QuizQuestionAndAnswer quizQuestionAndAnswer;

    @BeforeEach
    void setup() {
      userModel = makeMe.aUser().toModelPlease();
      notebook = makeMe.aNote().creatorAndOwner(userModel).please().getNotebook();
      makeMe.refresh(notebook);
    }

    @Test
    void shouldGetEmptyListOfNotes() throws UnexpectedNoAccessRightException {
      controller = new RestNotebookController(modelFactoryService, userModel, testabilitySettings);
      List<Note> result = controller.getNotes(notebook);
      assertThat(result.get(0).getQuizQuestionAndAnswers(), hasSize(0));
    }

    @Test
    void shouldGetListOfNotesWithQuestions() throws UnexpectedNoAccessRightException {
      controller = new RestNotebookController(modelFactoryService, userModel, testabilitySettings);
      QuizQuestionBuilder quizQuestionBuilder = makeMe.aQuestion();
      quizQuestionBuilder.approvedSpellingQuestionOf(notebook.getNotes().get(0)).please();
      List<Note> result = controller.getNotes(notebook);
      assertThat(result.get(0).getQuizQuestionAndAnswers(), hasSize(1));
    }
  }

  @Nested
  class getAllPendingRequestNotebooks {

    private Notebook notebook;

    @BeforeEach
    void setup() {
      UserModel userModel = makeMe.anAdmin().toModelPlease();
      notebook = makeMe.aNote().creatorAndOwner(userModel).please().getNotebook();
      makeMe.refresh(notebook);
    }

    @Test
    void shouldReturnPendingRequestNotebooks() {
      notebook.setApprovalStatus(ApprovalStatus.PENDING);
      List<Notebook> result = controller.getAllPendingRequestNotebooks();
      assertThat(result, hasSize(1));
    }

    @Test
    void shouldNotReturnApprovedNotebooks() {
      notebook.setApprovalStatus(ApprovalStatus.APPROVED);
      List<Notebook> result = controller.getAllPendingRequestNotebooks();
      assertThat(result, hasSize(0));
    }

    @Test
    void shouldApproveNoteBook() throws UnexpectedNoAccessRightException {
      Notebook result = controller.approveNoteBook(notebook);
      assertThat(result.getApprovalStatus(), equalTo(ApprovalStatus.APPROVED));
    }
  }
}
