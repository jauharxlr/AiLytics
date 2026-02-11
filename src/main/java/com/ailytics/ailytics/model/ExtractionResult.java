package com.ailytics.ailytics.model;

import lombok.Data;
import java.util.Map;

@Data
public class ExtractionResult {
    private Map<String, Object> data;
    private Double confidence;
}
