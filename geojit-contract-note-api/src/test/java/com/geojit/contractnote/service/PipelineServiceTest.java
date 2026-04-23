package com.geojit.contractnote.service;

import com.geojit.contractnote.entity.*;
import com.geojit.contractnote.repository.*;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PipelineServiceTest {

    @Mock PipelineEventRepository   pipelineEventRepository;
    @Mock JobCustomerRepository     jobCustomerRepository;
    @Mock JobRepository             jobRepository;
    @Mock EmailEventRepository      emailEventRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks PipelineService pipelineService;

    private UUID jobId;
    private String partyCode;
    private JobCustomer customer;

    @BeforeEach
    void setUp() {
        jobId     = UUID.randomUUID();
        partyCode = "PC001";
        customer  = new JobCustomer();
        customer.setPartyCode(partyCode);
        customer.setPdfStatus(JobCustomer.PdfStatus.PENDING);
        customer.setEmailStatus(JobCustomer.EmailStatus.PENDING);
    }

    @Test
    @DisplayName("PDF_GENERATED event sets customer PDF status to GENERATED")
    void processStatusEvent_pdfGenerated_updatesPdfStatus() {
        PipelineEvent event = PipelineEvent.builder()
                .jobId(jobId)
                .partyCode(partyCode)
                .eventType(PipelineEvent.EventType.PDF_GENERATED)
                .payload(Map.of("s3Key", "contractNote/2024/01/01/PC001/Geojit_test.pdf"))
                .lambdaName("create-pdf-geojit")
                .build();

        given(pipelineEventRepository.save(event)).willReturn(event);
        given(jobCustomerRepository.findByJob_JobIdAndPartyCode(jobId, partyCode))
                .willReturn(Optional.of(customer));
        given(jobCustomerRepository.save(any())).willReturn(customer);

        pipelineService.processStatusEvent(event);

        assertThat(customer.getPdfStatus()).isEqualTo(JobCustomer.PdfStatus.GENERATED);
        assertThat(customer.getPdfS3Key()).isEqualTo("contractNote/2024/01/01/PC001/Geojit_test.pdf");
        then(jobCustomerRepository).should().save(customer);
    }

    @Test
    @DisplayName("EMAIL_SENT event sets customer email status to SENT")
    void processStatusEvent_emailSent_updatesEmailStatus() {
        PipelineEvent event = PipelineEvent.builder()
                .jobId(jobId)
                .partyCode(partyCode)
                .eventType(PipelineEvent.EventType.EMAIL_SENT)
                .payload(Map.of("sesMessageId", "msg-abc-123"))
                .lambdaName("trigger-email-geojit")
                .build();

        given(pipelineEventRepository.save(event)).willReturn(event);
        given(jobCustomerRepository.findByJob_JobIdAndPartyCode(jobId, partyCode))
                .willReturn(Optional.of(customer));
        given(jobCustomerRepository.save(any())).willReturn(customer);

        pipelineService.processStatusEvent(event);

        assertThat(customer.getEmailStatus()).isEqualTo(JobCustomer.EmailStatus.SENT);
        assertThat(customer.getSesMessageId()).isEqualTo("msg-abc-123");
    }

    @Test
    @DisplayName("BOUNCE event sets customer email status to BOUNCED")
    void processStatusEvent_bounce_updatesEmailStatusToBounced() {
        PipelineEvent event = PipelineEvent.builder()
                .jobId(jobId)
                .partyCode(partyCode)
                .eventType(PipelineEvent.EventType.BOUNCE)
                .payload(Map.of("bounceType", "Permanent"))
                .lambdaName("pull-bounce-geojit")
                .build();

        given(pipelineEventRepository.save(event)).willReturn(event);
        given(jobCustomerRepository.findByJob_JobIdAndPartyCode(jobId, partyCode))
                .willReturn(Optional.of(customer));
        given(jobCustomerRepository.save(any())).willReturn(customer);

        pipelineService.processStatusEvent(event);

        assertThat(customer.getEmailStatus()).isEqualTo(JobCustomer.EmailStatus.BOUNCED);
        assertThat(customer.getBounceType()).isEqualTo("Permanent");
    }

    @Test
    @DisplayName("Event with null partyCode skips customer update")
    void processStatusEvent_nullPartyCode_skipsCustomerUpdate() {
        PipelineEvent event = PipelineEvent.builder()
                .jobId(jobId)
                .partyCode(null)
                .eventType(PipelineEvent.EventType.SPLIT_COMPLETE)
                .lambdaName("split-lambda-geojit")
                .build();

        given(pipelineEventRepository.save(event)).willReturn(event);

        pipelineService.processStatusEvent(event);

        then(jobCustomerRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("getJobEvents returns ordered list")
    void getJobEvents_returnsList() {
        List<PipelineEvent> events = List.of(
                PipelineEvent.builder().jobId(jobId).eventType(PipelineEvent.EventType.SPLIT_COMPLETE).lambdaName("l").build()
        );
        given(pipelineEventRepository.findByJobIdOrderByEventTimestampAsc(jobId)).willReturn(events);

        List<PipelineEvent> result = pipelineService.getJobEvents(jobId);
        assertThat(result).hasSize(1);
    }
}
