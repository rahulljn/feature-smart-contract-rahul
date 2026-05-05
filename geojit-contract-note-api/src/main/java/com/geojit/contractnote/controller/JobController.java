package com.geojit.contractnote.controller;

import com.geojit.contractnote.dto.request.ResendRequest;
import com.geojit.contractnote.dto.response.*;
import com.geojit.contractnote.entity.*;
import com.geojit.contractnote.repository.JobCustomerRepository;
import com.geojit.contractnote.repository.UserRepository;
import com.geojit.contractnote.service.AuditService;
import com.geojit.contractnote.service.CloudWatchService;
import com.geojit.contractnote.service.FileValidationService;
import com.geojit.contractnote.service.JobService;
import com.geojit.contractnote.service.PipelineService;
import com.geojit.contractnote.service.PipelineService.PipelineStatusAppEvent;
import com.geojit.contractnote.service.ResendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.context.event.EventListener;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@RestController
@RequestMapping("/api/v1/jobs")
@Tag(name = "Jobs")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class JobController {

    private final JobService              jobService;
    private final ResendService           resendService;
    private final PipelineService         pipelineService;
    private final CloudWatchService       cloudWatchService;
    private final UserRepository          userRepository;
    private final FileValidationService   fileValidationService;
    private final JobCustomerRepository   jobCustomerRepository;
    private final AuditService            auditService;

    // SSE emitters keyed by jobId — supports concurrent listeners
    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> emitters =
            new ConcurrentHashMap<>();

    // ─── Upload ───────────────────────────────────────────────────────────

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
    @Operation(summary = "Upload raw contract note file and create a new job")
    public ResponseEntity<ApiResponse<JobResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "segmentType", defaultValue = "EQUITY-COMBINEMARGIN") String segmentType,
            @RequestParam(value = "tradeDate", required = false) String tradeDate,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {

        validateUploadedFile(file);

        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        JobResponse job = jobService.uploadAndCreateJob(file, segmentType, tradeDate, user);
        auditService.log(user, AuditLog.AuditAction.UPLOAD, "Job:" + job.getJobId(),
                Map.of("fileName", job.getFileName() != null ? job.getFileName() : "",
                        "jobId", job.getJobId().toString(),
                        "segmentType", segmentType),
                request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Job created and pipeline triggered", job));
    }

    // ─── Pre-flight Validate ─────────────────────────────────────────────

    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
    @Operation(summary = "Pre-flight validate a raw file using Split + Invoke Lambda rules (no upload, summary only)")
    public ResponseEntity<ApiResponse<ValidationResultResponse>> validateFile(
            @RequestParam("file") MultipartFile file) throws Exception {
        validateUploadedFile(file);
        ValidationResultResponse result = fileValidationService.validate(file);
        return ResponseEntity.ok(ApiResponse.ok("Validation complete", result));
    }

    // ─── List / Get ───────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "List all jobs (paginated, optional status and date filter)")
    public ResponseEntity<ApiResponse<PageResponse<JobResponse>>> listJobs(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    Job.JobStatus status,
            @RequestParam(required = false)    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate from,
            @RequestParam(required = false)    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate to) {

        Pageable pageable = PageRequest.of(page, size);
        Page<JobResponse> result;
        if (from != null && to != null) {
            result = jobService.getJobsByDateRange(from, to, status, pageable);
        } else if (status != null) {
            result = jobService.getJobsByStatus(status, pageable);
        } else {
            result = jobService.getAllJobs(pageable);
        }
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result)));
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Get job detail by ID")
    public ResponseEntity<ApiResponse<JobResponse>> getJob(@PathVariable UUID jobId) {
        return ResponseEntity.ok(ApiResponse.ok(jobService.getJobById(jobId)));
    }

    @GetMapping("/{jobId}/customers")
    @Operation(summary = "Get paginated customer list for a job")
    public ResponseEntity<ApiResponse<PageResponse<JobCustomerResponse>>> getJobCustomers(
            @PathVariable UUID jobId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(jobService.getJobCustomers(jobId, pageable))));
    }

    @GetMapping("/{jobId}/pipeline-stats")
    @Operation(summary = "Get live pipeline stage counts, throughput rates, and latency metrics for a job")
    public ResponseEntity<ApiResponse<PipelineStatsResponse>> getPipelineStats(@PathVariable UUID jobId) {
        return ResponseEntity.ok(ApiResponse.ok(jobService.getPipelineStats(jobId)));
    }

    @GetMapping("/{jobId}/exception-counts")
    @Operation(summary = "Get accurate exception counts per type derived from live DB records (bypasses stale aggregate counters)")
    public ResponseEntity<ApiResponse<ExceptionCountsResponse>> getExceptionCounts(@PathVariable UUID jobId) {
        return ResponseEntity.ok(ApiResponse.ok(jobService.getExceptionCounts(jobId)));
    }

    @GetMapping("/{jobId}/cloudwatch-exceptions")
    @Operation(summary = "Get real-time Lambda exception logs from CloudWatch for a job, grouped per Lambda function")
    public ResponseEntity<ApiResponse<List<CloudWatchLambdaResponse>>> getCloudWatchExceptions(
            @PathVariable UUID jobId,
            @RequestParam(defaultValue = "ALL") String type) {
        return ResponseEntity.ok(ApiResponse.ok(cloudWatchService.getExceptionLogs(jobId, type)));
    }

    @GetMapping("/{jobId}/exceptions")
    @Operation(summary = "Get paginated exceptions for a job (PDF failed, email failed/bounced/skipped)")
    public ResponseEntity<ApiResponse<PageResponse<JobCustomerResponse>>> getExceptions(
            @PathVariable UUID jobId,
            @RequestParam(defaultValue = "ALL") String type,
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "50")  int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(jobService.getExceptions(jobId, type, pageable))));
    }

    // ─── Resend ───────────────────────────────────────────────────────────

    @PostMapping("/{jobId}/customers/{partyCode}/resend")
    @PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
    @Operation(summary = "Resend contract note for a single customer. Optional body: { overrideEmail, templateId }")
    public ResponseEntity<ApiResponse<Void>> resendCustomer(
            @PathVariable UUID jobId,
            @PathVariable String partyCode,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {

        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        String overrideEmail = (body != null) ? body.get("overrideEmail") : null;
        String templateIdStr = (body != null) ? body.get("templateId") : null;
        UUID templateId = (templateIdStr != null && !templateIdStr.isBlank()) ? UUID.fromString(templateIdStr) : null;
        resendService.resendForCustomer(jobId, partyCode, overrideEmail, templateId, user, httpRequest);
        return ResponseEntity.ok(ApiResponse.ok("Resend triggered for " + partyCode, null));
    }

    @PostMapping("/{jobId}/bulk-resend")
    @PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
    @Operation(summary = "Bulk resend all failed/bounced customers in a job. Optional body: { templateId }")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> bulkResend(
            @PathVariable UUID jobId,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {

        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        UUID templateId = resolveTemplateId(body);
        int queued = resendService.bulkResendFailed(jobId, user, httpRequest, templateId);
        return ResponseEntity.ok(ApiResponse.ok("Bulk resend triggered",
                Map.of("queued", queued)));
    }

    @PostMapping("/{jobId}/bulk-resend-codes")
    @PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
    @Operation(summary = "Bulk resend for specific party codes in a job. Body: { partyCodes: [string], templateId? }")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> bulkResendByCodes(
            @PathVariable UUID jobId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {

        @SuppressWarnings("unchecked")
        List<String> partyCodes = (List<String>) body.get("partyCodes");
        if (partyCodes == null || partyCodes.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok("No party codes provided", Map.of("queued", 0)));
        }
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        String templateIdStr = body.get("templateId") instanceof String s ? s : null;
        UUID templateId = (templateIdStr != null && !templateIdStr.isBlank()) ? UUID.fromString(templateIdStr) : null;
        int queued = resendService.bulkResendByPartyCodes(jobId, partyCodes, user, httpRequest, templateId);
        return ResponseEntity.ok(ApiResponse.ok("Bulk resend by codes triggered", Map.of("queued", queued)));
    }

    @PostMapping("/bulk-resend-all-bounced")
    @PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
    @Operation(summary = "Resend all BOUNCED customers across all jobs (global retry). Optional body: { templateId }")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> bulkResendAllBounced(
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {

        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        UUID templateId = resolveTemplateId(body);
        int queued = resendService.bulkResendAllBounced(user, httpRequest, templateId);
        return ResponseEntity.ok(ApiResponse.ok("Global bounce retry triggered", Map.of("queued", queued)));
    }

    @GetMapping("/failed-customers")
    @Operation(summary = "Paginated list of failed/bounced email customers across all jobs (for Resend page table)")
    public ResponseEntity<ApiResponse<PageResponse<JobCustomerResponse>>> getFailedCustomers(
            @RequestParam(defaultValue = "BOUNCED,FAILED") String statuses,
            @RequestParam(required = false) UUID jobId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {

        List<JobCustomer.EmailStatus> statusList = Arrays.stream(statuses.split(","))
                .map(s -> JobCustomer.EmailStatus.valueOf(s.trim()))
                .toList();

        LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDt   = to   != null ? to.plusDays(1).atStartOfDay() : null;

        Pageable pageable = PageRequest.of(page, size);
        Page<JobCustomerResponse> result = jobCustomerRepository
                .findGlobalByEmailStatuses(statusList, jobId, fromDt, toDt, pageable)
                .map(JobCustomerResponse::from);

        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result)));
    }

    private UUID resolveTemplateId(Map<String, String> body) {
        if (body == null) return null;
        String s = body.get("templateId");
        return (s != null && !s.isBlank()) ? UUID.fromString(s) : null;
    }

    // ─── SSE real-time pipeline stream ───────────────────────────────────

    /**
     * Server-Sent Events stream for real-time pipeline progress.
     *
     * Connect with: EventSource('/api/v1/jobs/{jobId}/stream', {headers: {Authorization: 'Bearer ...'}})
     *
     * Events are pushed here from the PipelineService when status events arrive.
     * The connection times out after 5 minutes; clients should reconnect.
     *
     * Timeout is intentionally short for this endpoint since Lambda events are
     * bursty — most jobs complete in < 2 minutes per chunk.
     */
    @GetMapping(value = "/{jobId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE stream for real-time pipeline events (poll-free live view)")
    public SseEmitter streamJobEvents(@PathVariable UUID jobId) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5 min timeout

        emitters.computeIfAbsent(jobId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // Skip full replay — just send connected so client knows stream is live.
        // Client fetches historical events via GET /pipeline-events separately.
        try {
            emitter.send(SseEmitter.event().name("connected").data("Stream ready"));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        emitter.onCompletion(()  -> removeEmitter(jobId, emitter));
        emitter.onTimeout(()     -> { emitter.complete(); removeEmitter(jobId, emitter); });
        emitter.onError(e        -> removeEmitter(jobId, emitter));

        return emitter;
    }

    /**
     * Called by PipelineService to push live events to connected SSE clients.
     * This is package-accessible — PipelineService calls this via a Spring bean reference.
     */
    public void broadcastEvent(UUID jobId, PipelineEvent event) {
        List<SseEmitter> jobEmitters = emitters.getOrDefault(jobId, new CopyOnWriteArrayList<>());
        List<SseEmitter> dead = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : jobEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .id(event.getEventId() != null ? event.getEventId().toString() : "")
                        .name(event.getEventType().name())
                        .data(Map.of(
                                "partyCode",      event.getPartyCode() != null ? event.getPartyCode() : "",
                                "eventType",      event.getEventType().name(),
                                "eventTimestamp", event.getEventTimestamp().toString(),
                                "payload",        event.getPayload() != null ? event.getPayload() : Map.of()
                        )));
            } catch (IOException e) {
                dead.add(emitter);
            }
        }
        jobEmitters.removeAll(dead);
    }

    // ─── helpers ─────────────────────────────────────────────────────────

    private void validateUploadedFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new com.geojit.contractnote.exception.ValidationException("Uploaded file is empty");
        }
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new com.geojit.contractnote.exception.ValidationException("File has no name");
        }
        String lower = filename.toLowerCase();
        if (!lower.endsWith(".txt") && !lower.endsWith(".csv")) {
            throw new com.geojit.contractnote.exception.ValidationException(
                    "Only .txt and .csv files are accepted. Got: " + filename);
        }
        if (file.getSize() > 10L * 1024 * 1024 * 1024) {
            throw new com.geojit.contractnote.exception.ValidationException(
                    "File size exceeds 10 GB limit");
        }
    }

    /** Listens for PipelineStatusAppEvent and pushes to connected SSE clients. */
    @EventListener
    public void onPipelineStatusEvent(PipelineStatusAppEvent appEvent) {
        broadcastEvent(appEvent.jobId(), appEvent.event());
    }

    private void removeEmitter(UUID jobId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(jobId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) emitters.remove(jobId);
        }
    }
}
