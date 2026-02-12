package com.ailytics.ailytics.service;

import com.ailytics.ailytics.model.ActionConfig;
import com.ailytics.ailytics.model.AutomationStep;
import com.ailytics.ailytics.repository.ActionConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MetadataService {

    private final ActionConfigRepository repository;

    @PostConstruct
    public void init() {
        if (repository.count() == 0) {
            // 1. MEDISEP Portal
            ActionConfig medisep = ActionConfig.builder()
                    .actionName("MEDISEP")
                    .loginUrl("https://medisep.example.com/login")
                    .extractionSchema("{ \"patient\": { \"name\": \"string\", \"id\": \"string\" }, \"claim\": { \"amount\": \"number\", \"date\": \"string\" } }")
                    .steps(List.of(
                            AutomationStep.builder().type(AutomationStep.StepType.NAVIGATE).targetUrl("https://medisep.example.com/claims/new").build(),
                            AutomationStep.builder().type(AutomationStep.StepType.FILL_FORM).fields(List.of("patient.name", "patient.id")).build(),
                            AutomationStep.builder().type(AutomationStep.StepType.CLICK).selector("Next Step").build(),
                            AutomationStep.builder().type(AutomationStep.StepType.FILL_FORM).fields(List.of("claim.amount", "claim.date")).build(),
                            AutomationStep.builder().type(AutomationStep.StepType.UPLOAD_FILE).selector("Upload Document").build(),
                            AutomationStep.builder().type(AutomationStep.StepType.CLICK).selector("Submit Final").build(),
                            AutomationStep.builder().type(AutomationStep.StepType.WAIT_FOR_LOAD).build(),
                            AutomationStep.builder().type(AutomationStep.StepType.CAPTURE_RESULT).selector(".claim-ack-id").build()
                    ))
                    .build();
            repository.save(medisep);

            // 2. CONTACT_US (Demo)
            ActionConfig contactUs = ActionConfig.builder()
                    .actionName("CONTACT_US")
                    .loginUrl("https://www.google.com")
                    .extractionSchema("{ \"fullName\": \"string\", \"emailAddress\": \"string\", \"subject\": \"string\", \"message\": \"string\" }")
                    .steps(List.of(
                            AutomationStep.builder().type(AutomationStep.StepType.NAVIGATE).targetUrl("https://formspree.io/library/contact-form/").build(),
                            AutomationStep.builder().type(AutomationStep.StepType.FILL_FORM).fields(List.of("fullName", "emailAddress", "subject", "message")).build(),
                            AutomationStep.builder().type(AutomationStep.StepType.CLICK).selector("Send").build(),
                            AutomationStep.builder().type(AutomationStep.StepType.WAIT_FOR_LOAD).build()
                    ))
                    .build();
            repository.save(contactUs);

            // 3. Multi-Stage Demo: BANK_TRANSFER_INIT (Stage 1)
            ActionConfig bankInit = ActionConfig.builder()
                    .actionName("BANK_TRANSFER_INIT")
                    .loginUrl("https://bank.example.com/login")
                    .extractionSchema("{ \"recipient\": \"string\", \"amount\": \"number\" }")
                    .steps(List.of(
                            AutomationStep.builder().type(AutomationStep.StepType.NAVIGATE).targetUrl("https://bank.example.com/transfer").build(),
                            AutomationStep.builder().type(AutomationStep.StepType.FILL_FORM).fields(List.of("recipient", "amount")).build(),
                            AutomationStep.builder().type(AutomationStep.StepType.CLICK).selector("Request OTP").build(),
                            AutomationStep.builder().type(AutomationStep.StepType.WAIT_FOR_LOAD).build(),
                            AutomationStep.builder().type(AutomationStep.StepType.CAPTURE_TO_CONTEXT).selector(".request-id").contextKey("transfer_id").build(),
                            AutomationStep.builder().type(AutomationStep.StepType.NEXT_RECIPE).nextRecipeName("BANK_TRANSFER_CONFIRM").build()
                    ))
                    .build();
            repository.save(bankInit);

            // 4. Multi-Stage Demo: BANK_TRANSFER_CONFIRM (Stage 2)
            ActionConfig bankConfirm = ActionConfig.builder()
                    .actionName("BANK_TRANSFER_CONFIRM")
                    .loginUrl("https://bank.example.com/login")
                    .steps(List.of(
                            AutomationStep.builder().type(AutomationStep.StepType.NAVIGATE).targetUrl("https://bank.example.com/confirm").build(),
                            // Injecting the ID captured in Stage 1
                            AutomationStep.builder().type(AutomationStep.StepType.FILL_FORM).fields(List.of("{{context.transfer_id}}")).build(),
                            AutomationStep.builder().type(AutomationStep.StepType.CLICK).selector("Confirm Transaction").build(),
                            AutomationStep.builder().type(AutomationStep.StepType.CAPTURE_RESULT).selector(".final-receipt").build()
                    ))
                    .build();
            repository.save(bankConfirm);
        }
    }

    public ActionConfig getConfig(String actionName) {
        return repository.findById(actionName).orElse(null);
    }

    public List<ActionConfig> getAllConfigs() {
        return repository.findAll();
    }

    public void saveConfig(ActionConfig config) {
        repository.save(config);
    }
}
