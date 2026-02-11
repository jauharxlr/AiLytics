package com.ailytics.ailytics.dto;

import lombok.Data;
import java.util.Map;

@Data
public class AutomationRequest {
    private String url;
    private String username;
    private String password;
    private Map<String, String> formData;
}
