package com.ailytics.ailytics.repository;

import com.ailytics.ailytics.model.WorkflowResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowResultRepository extends JpaRepository<WorkflowResult, String> {
}
