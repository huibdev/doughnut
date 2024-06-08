package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.factories.AiQuestionFactory;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.theokanning.openai.client.OpenAiApi;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class QuizQuestionService {
  private final ModelFactoryService modelFactoryService;

  private AiQuestionGenerator aiQuestionGenerator;

  public QuizQuestionService(OpenAiApi openAiApi, ModelFactoryService modelFactoryService) {
    this.modelFactoryService = modelFactoryService;
    this.aiQuestionGenerator =
        new AiQuestionGenerator(openAiApi, new GlobalSettingsService(modelFactoryService));
  }

  public QuizQuestion generateAIQuestion(Note note) {
    AiQuestionFactory aiQuestionFactory = new AiQuestionFactory(note, aiQuestionGenerator);
    try {
      QuizQuestion quizQuestion = aiQuestionFactory.buildValidQuizQuestion(null);
      modelFactoryService.save(quizQuestion);
      return quizQuestion;
    } catch (QuizQuestionNotPossibleException e) {
      throw (new ResponseStatusException(HttpStatus.NOT_FOUND, "No question generated"));
    }
  }

  public List<QuizQuestion> getPendingQuestionsByHeadNote(Note headNote) {
    return modelFactoryService.getQuizQuestionsByHeadNote(headNote).stream()
        .filter(x -> !x.isApproved() && !x.isReviewed())
        .toList();
  }

  QuizQuestion selectQuizQuestionForANote(Note note) {
    return this.modelFactoryService.getQuizQuestionsByNote(note).stream()
        .filter(q -> q.approved)
        .findFirst()
        .orElse(null);
  }
}
