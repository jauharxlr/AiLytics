package com.ailytics.ailytics.service;

import com.ailytics.ailytics.model.ActionConfig;
import com.ailytics.ailytics.model.ExtractionResult;
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

    @Value("${webhooks.min-confidence:0.85}")
    private double minConfidence;

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

            // Phase 1: Extraction with Confidence Scoring
            log.info("Starting extraction for job: {}", job.getJobId());
            ExtractionResult extraction = geminiService.extractDataWithConfidence(resource, job.getContentType(), config.getExtractionSchema());
            
            job.setConfidenceScore(extraction.getConfidence());
            job.setExtractedData(extraction.getData());

            // Approval Gate check
            if (extraction.getConfidence() < minConfidence) {
                log.warn("Confidence {} below threshold {} for job: {}. Transitioning to AWAITING_APPROVAL", 
                        extraction.getConfidence(), minConfidence, job.getJobId());
                job.setStatus(ProcessingQueue.JobStatus.AWAITING_APPROVAL);
                job.setNeedsApproval(true);
                broadcastWebhook(job);
                return;
            }

            // Phase 2: Automation (Continue if confidence is high)
            continueToAutomation(job, config);

        } catch (Exception e) {
            log.error("Job failed: {}", job.getJobId(), e);
            job.setStatus(ProcessingQueue.JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            broadcastWebhook(job);
        } finally {
            queueRepository.save(job);
        }
    }

    private void continueToAutomation(ProcessingQueue job, ActionConfig config) {
        log.info("Starting automation for job: {}", job.getJobId());
        String result = portalBridgeService.executeAutomation(
                job.getJobId(), 
                config, 
                job.getExtractedData(), 
                job.getUsername(), 
                job.getPassword(), 
                Paths.get(job.getFilePath())
        );

        job.setStatus(ProcessingQueue.JobStatus.COMPLETED);
        job.setResultId(result);
        broadcastWebhook(job);
    }

    public void approveAndResume(String jobId, Map<String, Object> correctedData) {
        ProcessingQueue job = queueRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job not found"));
        if (job.getStatus() != ProcessingQueue.JobStatus.AWAITING_APPROVAL) {
            throw new RuntimeException("Job is not awaiting approval");
        }

        job.setExtractedData(correctedData);
        job.setStatus(ProcessingQueue.JobStatus.PROCESSING);
        job.setNeedsApproval(false);
        queueRepository.save(job);

        executorService.submit(() -> {
            try {
                ActionConfig config = metadataService.getConfig(job.getActionName());
                continueToAutomation(job, config);
            } catch (Exception e) {
                log.error("Resumed job failed: {}", job.getJobId(), e);
                job.setStatus(ProcessingQueue.JobStatus.FAILED);
                job.setErrorMessage(e.getMessage());
                broadcastWebhook(job);
            } finally {
                queueRepository.save(job);
            }
        });
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

    public List<ProcessingQueue> getAllJobs() {
        return queueRepository.findAllByOrderByCreatedAtDesc();
    }

    public void retryJob(String jobId) {
        ProcessingQueue job = queueRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job not found"));
        if (job.getExtractedData() == null) {
            throw new RuntimeException("Cannot retry: No extracted data found. Please re-upload.");
        }

        log.info("Retrying job: {} from existing extraction", jobId);
        job.setStatus(ProcessingQueue.JobStatus.PROCESSING);
        job.setErrorMessage(null);
        job.setResultId(null);
        queueRepository.save(job);

        executorService.submit(() -> {
            try {
                ActionConfig config = metadataService.getConfig(job.getActionName());
                continueToAutomation(job, config);
            } catch (Exception e) {
                log.error("Retried job failed: {}", job.getJobId(), e);
                job.setStatus(ProcessingQueue.JobStatus.FAILED);
                job.setErrorMessage(e.getMessage());
                broadcastWebhook(job);
            } finally {
                queueRepository.save(job);
            }
        });
    }
}
