package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.json.FineTuningExampleForQuestionGeneration;
import com.odde.doughnut.entities.SuggestedQuestionForFineTuning;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/fine-tuning")
class RestFineTuningDataController {
  private final ModelFactoryService modelFactoryService;
  private UserModel currentUser;

  public RestFineTuningDataController(
      ModelFactoryService modelFactoryService, UserModel currentUser) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
  }

  @GetMapping("/question-generation-examples")
  public List<FineTuningExampleForQuestionGeneration> getAllQuestionGenerationFineTuningExamples()
      throws UnexpectedNoAccessRightException {
    currentUser.assertAdminAuthorization();
    return getSuggestedQuestionForFineTunings().stream()
        .map(SuggestedQuestionForFineTuning::toFineTuningExample)
        .toList();
  }

  @GetMapping("/all-suggested-questions-for-fine-tuning")
  public List<SuggestedQuestionForFineTuning> getAllSuggestedQuestions()
      throws UnexpectedNoAccessRightException {
    currentUser.assertAdminAuthorization();
    return getSuggestedQuestionForFineTunings();
  }

  private List<SuggestedQuestionForFineTuning> getSuggestedQuestionForFineTunings() {
    List<SuggestedQuestionForFineTuning> suggestedQuestionForFineTunings = new ArrayList<>();
    modelFactoryService
        .questionSuggestionForFineTuningRepository
        .findAll()
        .forEach(suggestedQuestionForFineTunings::add);
    return suggestedQuestionForFineTunings;
  }
}