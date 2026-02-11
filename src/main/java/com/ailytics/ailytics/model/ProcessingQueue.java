package com.ailytics.ailytics.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingQueue {
    @Id
    private String jobId;
    private String actionName;
    private String filePath;
    private String contentType;
    private String username;
    private String password;
    
    @Enumerated(EnumType.STRING)
    private JobStatus status;
    
    private String resultId;
    private String errorMessage;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum JobStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
