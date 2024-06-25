package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.UserAssistantThread;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.AiAdvisorWithStorageService;
import com.odde.doughnut.services.ai.AssistantService;
import com.odde.doughnut.testability.TestabilitySettings;
import com.theokanning.openai.assistants.message.Message;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.service.assistant_stream.AssistantSSE;
import io.reactivex.Flowable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Resource;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@SessionScope
@RequestMapping("/api/ai")
public class RestAiController {

  private final UserModel currentUser;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  private final AiAdvisorWithStorageService aiAdvisorWithStorageService;

  public RestAiController(
      @Qualifier("testableOpenAiApi") OpenAiApi openAiApi,
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      TestabilitySettings testabilitySettings) {
    this.aiAdvisorWithStorageService =
        new AiAdvisorWithStorageService(openAiApi, modelFactoryService);
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
  }

  @PostMapping("/{note}/completion")
  @Transactional
  public AiAssistantResponse getCompletion(
      @PathVariable(name = "note") @Schema(type = "integer") Note note,
      @RequestBody AiCompletionParams aiCompletionParams) {
    currentUser.assertLoggedIn();
    return aiAdvisorWithStorageService
        .getContentCompletionService()
        .createThreadAndRunWithFirstMessage(note, aiCompletionParams.getCompletionPrompt());
  }

  @PostMapping("/answer-clarifying-question")
  @Transactional
  public AiAssistantResponse answerCompletionClarifyingQuestion(
      @RequestBody AiCompletionAnswerClarifyingQuestionParams answerClarifyingQuestionParams) {
    currentUser.assertLoggedIn();
    return aiAdvisorWithStorageService
        .getContentCompletionService()
        .answerAiCompletionClarifyingQuestion(answerClarifyingQuestionParams);
  }

  @GetMapping("/chat/{note}")
  public List<Message> tryRestoreChat(
      @PathVariable(value = "note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException {
    currentUser.assertReadAuthorization(note);
    AssistantService assistantService = aiAdvisorWithStorageService.getChatService();
    UserAssistantThread byUserAndNote =
        aiAdvisorWithStorageService
            .modelFactoryService()
            .userAssistantThreadRepository
            .findByUserAndNote(currentUser.getEntity(), note);
    if (byUserAndNote == null) {
      return List.of();
    }
    return assistantService.loadPreviousMessages(byUserAndNote.getThreadId());
  }

  @PostMapping(path = "/chat/{note}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @Transactional
  public SseEmitter chat(
      @PathVariable(value = "note") @Schema(type = "integer") Note note,
      @RequestBody ChatRequest request)
      throws UnexpectedNoAccessRightException {
    currentUser.assertReadAuthorization(note);
    AssistantService assistantService = aiAdvisorWithStorageService.getChatService();
    SseEmitter emitter = new SseEmitter();
    String threadId = request.getThreadId();
    if (threadId == null) {
      threadId = assistantService.createThread(note);
      UserAssistantThread userAssistantThread = new UserAssistantThread();
      userAssistantThread.setThreadId(threadId);
      userAssistantThread.setNote(note);
      userAssistantThread.setUser(currentUser.getEntity());
      aiAdvisorWithStorageService.modelFactoryService().entityManager.persist(userAssistantThread);
    }
    Flowable<AssistantSSE> runStream =
        assistantService.createMessageRunAndGetResponseStream(request.getUserMessage(), threadId);
    runStream.subscribe(
        sse -> {
          try {
            SseEmitter.SseEventBuilder builder =
                SseEmitter.event().name(sse.getEvent().eventName).data(sse.getData());
            emitter.send(builder);
          } catch (Exception e) {
            emitter.completeWithError(e);
          }
        });
    return emitter;
  }

  @GetMapping("/dummy")
  public DummyForGeneratingTypes dummyEntryToGenerateDataTypesThatAreRequiredInEventStream()
      throws HttpMediaTypeNotAcceptableException {
    throw new HttpMediaTypeNotAcceptableException("dummy");
  }

  @PostMapping("/generate-image")
  @Transactional
  public AiGeneratedImage generateImage(@RequestBody String prompt) {
    currentUser.assertLoggedIn();
    return new AiGeneratedImage(
        aiAdvisorWithStorageService.aiAdvisorService().getOtherAiServices().getTimage(prompt));
  }

  @GetMapping("/available-gpt-models")
  public List<String> getAvailableGptModels() {
    return aiAdvisorWithStorageService
        .aiAdvisorService()
        .getOtherAiServices()
        .getAvailableGptModels();
  }

  @PostMapping("/recreate-all-assistants")
  @Transactional
  public Map<String, String> recreateAllAssistants() throws UnexpectedNoAccessRightException {
    currentUser.assertAdminAuthorization();
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    return aiAdvisorWithStorageService.recreateAllAssistants(currentUTCTimestamp);
  }
}
