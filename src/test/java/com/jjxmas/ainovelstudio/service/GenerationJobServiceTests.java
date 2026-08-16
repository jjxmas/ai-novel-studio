package com.jjxmas.ainovelstudio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jjxmas.ainovelstudio.mapper.GenerationJobMapper;
import com.jjxmas.ainovelstudio.pojo.entity.GenerationJob;
import com.jjxmas.ainovelstudio.service.impl.GenerationJobServiceImpl;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GenerationJobServiceTests {

    private GenerationJobMapper mapper;
    private GenerationJobServiceImpl service;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        mapper = mock(GenerationJobMapper.class);
        service = new GenerationJobServiceImpl(mapper);
        now = LocalDateTime.of(2026, 8, 16, 15, 30);
        when(mapper.insertPending(any(GenerationJob.class))).thenAnswer(invocation -> {
            GenerationJob job = invocation.getArgument(0);
            job.setId(10L);
            return 1;
        });
    }

    @Test
    void enqueueCreatesPendingUnclaimedJob() {
        Long jobId = service.enqueueJob(
                1L, "chapter_post_process", "chapter", 20L, 3L, Map.of("chapterId", 20L), "20:4", 5, now);

        assertThat(jobId).isEqualTo(10L);
        ArgumentCaptor<GenerationJob> inserted = ArgumentCaptor.forClass(GenerationJob.class);
        verify(mapper).insertPending(inserted.capture());
        assertThat(inserted.getValue().getStatus()).isEqualTo("pending");
        assertThat(inserted.getValue().getDedupeKey()).isEqualTo("20:4");
        assertThat(inserted.getValue().getAttemptCount()).isZero();
        assertThat(inserted.getValue().getLockedBy()).isNull();
        assertThat(inserted.getValue().getScheduledAt()).isEqualTo(now);
    }

    @Test
    void claimMarksJobRunningAndIncrementsAttempt() {
        GenerationJob pending = new GenerationJob()
                .setStatus("pending")
                .setAttemptCount(1);
        pending.setId(10L);
        when(mapper.selectNextClaimable(now)).thenReturn(pending);

        GenerationJob claimed = service.claimNext("worker-a", now);

        assertThat(claimed.getStatus()).isEqualTo("running");
        assertThat(claimed.getLockedBy()).isEqualTo("worker-a");
        assertThat(claimed.getLockedAt()).isEqualTo(now);
        assertThat(claimed.getStartedAt()).isEqualTo(now);
        assertThat(claimed.getAttemptCount()).isEqualTo(2);
        verify(mapper).updateById(pending);
    }

    @Test
    void completeRequiresCurrentWorkerOwnership() {
        when(mapper.completeClaim(eq(10L), eq("worker-a"), any(), eq(now))).thenReturn(0);

        assertThatThrownBy(() -> service.completeJob(10L, "worker-a", Map.of("ok", true), now))
                .hasMessage("GENERATION_JOB_CLAIM_CONFLICT");
    }

    @Test
    void retryFailureReturnsJobToPendingAtRequestedTime() {
        LocalDateTime retryAt = now.plusMinutes(2);
        when(mapper.failClaim(10L, "worker-a", "pending", "timeout", retryAt, null)).thenReturn(1);

        service.failJob(10L, "worker-a", "timeout", true, retryAt, now);

        verify(mapper).failClaim(10L, "worker-a", "pending", "timeout", retryAt, null);
    }

    @Test
    void recoverExpiredJobsDelegatesLeaseCutoff() {
        LocalDateTime cutoff = now.minusMinutes(5);
        when(mapper.recoverExpired(cutoff, now)).thenReturn(3);

        assertThat(service.recoverExpiredJobs(cutoff, now)).isEqualTo(3);
    }
}
