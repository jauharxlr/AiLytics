package com.ailytics.ailytics.service;

import com.ailytics.ailytics.model.ActionConfig;
import com.ailytics.ailytics.model.AutomationStep;
import com.ailytics.ailytics.repository.ActionConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MetadataService {

    private final ActionConfigRepository repository;

    @PostConstruct
    public void init() {
        if (repository.count() == 0) {
            // Seed MEDISEP Configuration as a Wizard Flow
            ActionConfig medisep = ActionConfig.builder()
                    .actionName("MEDISEP")
                    .loginUrl("https://medisep.example.com/login")
                    .usernameSelector("#uid")
                    .passwordSelector("#pwd")
                    .loginSubmitSelector(".btn-login")
                    .extractionSchema("{ \"patient\": { \"name\": \"string\", \"id\": \"string\" }, \"claim\": { \"amount\": \"number\", \"date\": \"string\" } }")
                    .steps(List.of(
                            AutomationStep.builder()
                                    .type(AutomationStep.StepType.NAVIGATE)
                                    .targetUrl("https://medisep.example.com/claims/new")
                                    .build(),
                            AutomationStep.builder()
                                    .type(AutomationStep.StepType.FILL_FORM)
                                    .fieldMapping(Map.of(
                                            "patient.name", "#p-name",
                                            "patient.id", "#p-id"
                                    ))
                                    .build(),
                            AutomationStep.builder().type(AutomationStep.StepType.CLICK).selector("#next-step").build(),
                            AutomationStep.builder()
                                    .type(AutomationStep.StepType.FILL_FORM)
                                    .fieldMapping(Map.of(
                                            "claim.amount", "input[name='amt']",
                                            "claim.date", "#date-picker"
                                    ))
                                    .build(),
                            AutomationStep.builder()
                                    .type(AutomationStep.StepType.UPLOAD_FILE)
                                    .selector("#upload-doc")
                                    .build(),
                            AutomationStep.builder().type(AutomationStep.StepType.CLICK).selector("#submit-final").build(),
                            AutomationStep.builder().type(AutomationStep.StepType.WAIT_FOR_LOAD).build(),
                            AutomationStep.builder().type(AutomationStep.StepType.CAPTURE_RESULT).selector(".claim-ack-id").build()
                    ))
                    .build();

            repository.save(medisep);
        }
    }

    public ActionConfig getConfig(String actionName) {
        return repository.findById(actionName).orElse(null);
    }

    public void saveConfig(ActionConfig config) {
        repository.save(config);
    }
}
