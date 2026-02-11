package com.ailytics.ailytics.service;

import com.ailytics.ailytics.model.ActionConfig;
import com.ailytics.ailytics.model.ProcessingQueue;
import com.ailytics.ailytics.repository.ProcessingQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkflowService {

    private final MetadataService metadataService;
    private final GeminiService geminiService;
    private final PortalBridgeService portalBridgeService;
    private final ProcessingQueueRepository queueRepository;
    private final FileStorageService fileStorageService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${processing.max-concurrent-jobs:5}")
    private int maxConcurrentJobs;

    @Value("${webhooks.urls:}")
    private List<String> webhookUrls;

    private Semaphore semaphore;
    private ExecutorService executorService;

    @jakarta.annotation.PostConstruct
    public void init() {
        this.semaphore = new Semaphore(maxConcurrentJobs);
        this.executorService = Executors.newFixedThreadPool(maxConcurrentJobs);
    }

    public String enqueueWorkflow(MultipartFile file, String actionName, String username, String password) {
        String jobId = UUID.randomUUID().toString();
        String storedPath = fileStorageService.store(file);

        ProcessingQueue job = ProcessingQueue.builder()
                .jobId(jobId)
                .actionName(actionName)
                .filePath(storedPath)
                .contentType(file.getContentType())
                .username(username)
                .password(password)
                .status(ProcessingQueue.JobStatus.PENDING)
                .build();

        queueRepository.save(job);
        log.info("Enqueued job: {} for action: {}", jobId, actionName);
        return jobId;
    }

    @Scheduled(fixedDelay = 5000)
    public void processQueue() {
        List<ProcessingQueue> pendingJobs = queueRepository.findByStatusOrderByCreatedAtAsc(ProcessingQueue.JobStatus.PENDING);
        
        for (ProcessingQueue job : pendingJobs) {
            if (semaphore.tryAcquire()) {
                job.setStatus(ProcessingQueue.JobStatus.PROCESSING);
                queueRepository.save(job);
                
                executorService.submit(() -> {
                    try {
                        executeWorkflow(job);
                    } finally {
                        semaphore.release();
                    }
                });
            }
        }
    }

    private void executeWorkflow(ProcessingQueue job) {
        try {
            ActionConfig config = metadataService.getConfig(job.getActionName());
            if (config == null) throw new RuntimeException("Action config not found");

            Resource resource = fileStorageService.getResource(job.getFilePath());

            // Phase 1: Extraction
            log.info("Starting extraction for job: {}", job.getJobId());
            Map<String, Object> extractedData = geminiService.extractData(resource, job.getContentType(), config.getExtractionSchema());

            // Phase 2: Automation
            log.info("Starting automation for job: {}", job.getJobId());
            String result = portalBridgeService.executeAutomation(job.getJobId(), config, extractedData, job.getUsername(), job.getPassword(), Paths.get(job.getFilePath()));

            job.setStatus(ProcessingQueue.JobStatus.COMPLETED);
            job.setResultId(result);
            broadcastWebhook(job);
        } catch (Exception e) {
            log.error("Job failed: {}", job.getJobId(), e);
            job.setStatus(ProcessingQueue.JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            broadcastWebhook(job);
        } finally {
            queueRepository.save(job);
        }
    }

    private void broadcastWebhook(ProcessingQueue job) {
        if (webhookUrls == null || webhookUrls.isEmpty()) return;
        
        for (String url : webhookUrls) {
            try {
                restTemplate.postForEntity(url, job, String.class);
                log.info("Webhook sent to: {} for job: {}", url, job.getJobId());
            } catch (Exception e) {
                log.error("Failed to send webhook to: {}", url, e);
            }
        }
    }

    public ProcessingQueue getJobStatus(String jobId) {
        return queueRepository.findById(jobId).orElse(null);
    }
}
