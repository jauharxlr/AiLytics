package com.ailytics.ailytics.controller;

import com.ailytics.ailytics.model.ActionConfig;
import com.ailytics.ailytics.service.MetadataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/actions")
@RequiredArgsConstructor
@Tag(name = "Admin Actions", description = "Endpoints for managing portal automation recipes")
@org.springframework.security.access.prepost.PreAuthorize("hasRole('MASTER')")
public class ActionAdminController {

    private final MetadataService metadataService;

    @Operation(summary = "List all automation recipes")
    @GetMapping
    public ResponseEntity<List<ActionConfig>> listActions() {
        return ResponseEntity.ok(metadataService.getAllConfigs());
    }

    @Operation(summary = "Register or update an automation recipe")
    @PostMapping
    public ResponseEntity<ActionConfig> registerAction(@RequestBody ActionConfig config) {
        metadataService.saveConfig(config);
        return ResponseEntity.ok(config);
    }

    @Operation(summary = "Get action details")
    @GetMapping("/{actionName}")
    public ResponseEntity<ActionConfig> getAction(@PathVariable String actionName) {
        ActionConfig config = metadataService.getConfig(actionName);
        return config != null ? ResponseEntity.ok(config) : ResponseEntity.notFound().build();
    }
}
