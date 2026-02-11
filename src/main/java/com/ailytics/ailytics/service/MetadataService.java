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
            // Seed MEDISEP Configuration as a PURELY SEMANTIC Wizard Flow
            ActionConfig medisep = ActionConfig.builder()
                    .actionName("MEDISEP")
                    .loginUrl("https://medisep.example.com/login")
                    .extractionSchema("{ \"patient\": { \"name\": \"string\", \"id\": \"string\" }, \"claim\": { \"amount\": \"number\", \"date\": \"string\" } }")
                    .steps(List.of(
                            AutomationStep.builder()
                                    .type(AutomationStep.StepType.NAVIGATE)
                                    .targetUrl("https://medisep.example.com/claims/new")
                                    .build(),
                            AutomationStep.builder()
                                    .type(AutomationStep.StepType.FILL_FORM)
                                    .fields(List.of("patient.name", "patient.id"))
                                    .build(),
                            AutomationStep.builder().type(AutomationStep.StepType.CLICK).selector("Next Step").build(),
                            AutomationStep.builder()
                                    .type(AutomationStep.StepType.FILL_FORM)
                                    .fields(List.of("claim.amount", "claim.date"))
                                    .build(),
                            AutomationStep.builder()
                                    .type(AutomationStep.StepType.UPLOAD_FILE)
                                    .selector("Upload Document")
                                    .build(),
                            AutomationStep.builder().type(AutomationStep.StepType.CLICK).selector("Submit Final").build(),
                            AutomationStep.builder().type(AutomationStep.StepType.WAIT_FOR_LOAD).build(),
                            AutomationStep.builder().type(AutomationStep.StepType.CAPTURE_RESULT).selector(".claim-ack-id").build()
                    ))
                    .build();

            repository.save(medisep);

            // Seed CONTACT_US Example for Quick Testing
            ActionConfig contactUs = ActionConfig.builder()
                    .actionName("CONTACT_US")
                    .loginUrl("https://www.google.com") // Dummy login, we'll navigate away
                    .extractionSchema("{ \"fullName\": \"string\", \"emailAddress\": \"string\", \"subject\": \"string\", \"message\": \"string\" }")
                    .steps(List.of(
                            AutomationStep.builder()
                                    .type(AutomationStep.StepType.NAVIGATE)
                                    .targetUrl("https://formspree.io/library/contact-form/") // A common public demo form
                                    .build(),
                            AutomationStep.builder()
                                    .type(AutomationStep.StepType.FILL_FORM)
                                    .fields(List.of("fullName", "emailAddress", "subject", "message"))
                                    .build(),
                            AutomationStep.builder()
                                    .type(AutomationStep.StepType.CLICK)
                                    .selector("Send")
                                    .build(),
                            AutomationStep.builder().type(AutomationStep.StepType.WAIT_FOR_LOAD).build()
                    ))
                    .build();
            repository.save(contactUs);
        }
    }

    public ActionConfig getConfig(String actionName) {
        return repository.findById(actionName).orElse(null);
    }

    public void saveConfig(ActionConfig config) {
        repository.save(config);
    }
}
