package com.ailytics.ailytics.repository;

import com.ailytics.ailytics.model.ProcessingQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProcessingQueueRepository extends JpaRepository<ProcessingQueue, String> {
    List<ProcessingQueue> findByStatusOrderByCreatedAtAsc(ProcessingQueue.JobStatus status);
}
