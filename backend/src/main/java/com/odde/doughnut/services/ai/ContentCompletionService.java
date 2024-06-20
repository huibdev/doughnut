package com.odde.doughnut.services.ai;

import static com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder.askClarificationQuestion;
import static com.odde.doughnut.services.ai.tools.AiToolFactory.COMPLETE_NOTE_DETAILS;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.assistants.message.MessageRequest;
import com.theokanning.openai.assistants.run.RequiredAction;
import com.theokanning.openai.assistants.run.Run;
import com.theokanning.openai.assistants.run.ToolCall;
import com.theokanning.openai.assistants.thread.Thread;
import com.theokanning.openai.assistants.thread.ThreadRequest;
import java.util.List;

public record ContentCompletionService(OpenAiApiHandler openAiApiHandler) {
  public AiAssistantResponse initiateAThread(Note note, String assistantId, String prompt) {
    String threadId = createThread(note, prompt);
    Run run = openAiApiHandler.createRun(threadId, assistantId);
    return getThreadResponse(threadId, run);
  }

  public AiAssistantResponse answerAiCompletionClarifyingQuestion(
      AiCompletionAnswerClarifyingQuestionParams answerClarifyingQuestionParams) {
    String threadId = answerClarifyingQuestionParams.getThreadId();

    Run retrievedRun = openAiApiHandler.submitToolOutputs(answerClarifyingQuestionParams);

    return getThreadResponse(threadId, retrievedRun);
  }

  private String createThread(Note note, String completionPrompt) {
    ThreadRequest threadRequest = ThreadRequest.builder().build();
    Thread thread = openAiApiHandler.createThread(threadRequest);
    MessageRequest messageRequest =
        MessageRequest.builder()
            .content(note.getNoteDescription() + "------------\n" + completionPrompt)
            .build();

    openAiApiHandler.createMessage(thread.getId(), messageRequest);
    return thread.getId();
  }

  private AiAssistantResponse getThreadResponse(String threadId, Run currentRun) {
    Run run = openAiApiHandler.retrieveUntilCompletedOrRequiresAction(threadId, currentRun);

    AiAssistantResponse completionResponse = new AiAssistantResponse();
    completionResponse.setThreadId(threadId);
    completionResponse.setRunId(currentRun.getId());

    if (run.getStatus().equals("requires_action")) {
      RequiredAction requiredAction = run.getRequiredAction();
      int size = requiredAction.getSubmitToolOutputs().getToolCalls().size();
      if (size != 1) {
        throw new RuntimeException("Unexpected number of tool calls: " + size);
      }
      ToolCall toolCall = requiredAction.getSubmitToolOutputs().getToolCalls().getFirst();

      AiCompletionRequiredAction actionRequired =
          getTools()
              .flatMap(t -> t.tryConsume(toolCall))
              .findFirst()
              .orElseThrow(
                  () ->
                      new RuntimeException(
                          "Unknown function name: " + toolCall.getFunction().getName()));

      actionRequired.setToolCallId(toolCall.getId());

      completionResponse.setRequiredAction(actionRequired);
    } else {
      String message =
          openAiApiHandler
              .getThreadLastMessage(threadId)
              .getContent()
              .getFirst()
              .getText()
              .getValue();
      completionResponse.setLastMessage(message);
    }
    return completionResponse;
  }

  public static List<AiTool> getTools() {
    return List.of(
        AiTool.build(
            COMPLETE_NOTE_DETAILS,
            "Text completion for the details of the note of focus",
            NoteDetailsCompletion.class,
            (noteDetailsCompletion) -> {
              AiCompletionRequiredAction result = new AiCompletionRequiredAction();
              result.setContentToAppend(noteDetailsCompletion.completion);
              return result;
            }),
        AiTool.build(
            askClarificationQuestion,
            "Ask question to get more context",
            ClarifyingQuestion.class,
            (clarifyingQuestion) -> {
              AiCompletionRequiredAction result = new AiCompletionRequiredAction();
              result.setClarifyingQuestion(clarifyingQuestion);
              return result;
            }));
  }

  public static List<AiTool> getChatTools() {
    return List.of();
  }
}
