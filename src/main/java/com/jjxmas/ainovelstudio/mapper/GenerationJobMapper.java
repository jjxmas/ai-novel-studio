package com.jjxmas.ainovelstudio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jjxmas.ainovelstudio.pojo.entity.GenerationJob;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface GenerationJobMapper extends BaseMapper<GenerationJob> {

    @Insert("""
            INSERT INTO generation_jobs (
                project_id, job_type, related_entity_type, related_entity_id, dedupe_key,
                model_config_id, status, priority, attempt_count, input_snapshot, scheduled_at
            ) VALUES (
                #{projectId}, #{jobType}, #{relatedEntityType}, #{relatedEntityId}, #{dedupeKey},
                #{modelConfigId}, #{status}, #{priority}, #{attemptCount}, #{inputSnapshot}, #{scheduledAt}
            )
            ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertPending(GenerationJob job);

    @Select("""
            SELECT *
            FROM generation_jobs
            WHERE status = 'pending'
              AND (scheduled_at IS NULL OR scheduled_at <= #{now})
            ORDER BY priority DESC, created_at ASC
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """)
    GenerationJob selectNextClaimable(@Param("now") LocalDateTime now);

    @Update("""
            UPDATE generation_jobs
            SET locked_at = #{now}
            WHERE id = #{jobId}
              AND status = 'running'
              AND locked_by = #{workerId}
            """)
    int heartbeat(
            @Param("jobId") Long jobId,
            @Param("workerId") String workerId,
            @Param("now") LocalDateTime now);

    @Update("""
            UPDATE generation_jobs
            SET status = 'succeeded',
                output_snapshot = #{outputSnapshot},
                error_message = NULL,
                locked_by = NULL,
                locked_at = NULL,
                finished_at = #{now}
            WHERE id = #{jobId}
              AND status = 'running'
              AND locked_by = #{workerId}
            """)
    int completeClaim(
            @Param("jobId") Long jobId,
            @Param("workerId") String workerId,
            @Param("outputSnapshot") String outputSnapshot,
            @Param("now") LocalDateTime now);

    @Update("""
            UPDATE generation_jobs
            SET status = #{nextStatus},
                error_message = #{errorMessage},
                locked_by = NULL,
                locked_at = NULL,
                scheduled_at = #{scheduledAt},
                finished_at = #{finishedAt}
            WHERE id = #{jobId}
              AND status = 'running'
              AND locked_by = #{workerId}
            """)
    int failClaim(
            @Param("jobId") Long jobId,
            @Param("workerId") String workerId,
            @Param("nextStatus") String nextStatus,
            @Param("errorMessage") String errorMessage,
            @Param("scheduledAt") LocalDateTime scheduledAt,
            @Param("finishedAt") LocalDateTime finishedAt);

    @Update("""
            UPDATE generation_jobs
            SET status = 'pending',
                locked_by = NULL,
                locked_at = NULL,
                scheduled_at = #{now},
                error_message = 'WORKER_LEASE_EXPIRED'
            WHERE status = 'running'
              AND locked_at < #{lockedBefore}
            """)
    int recoverExpired(
            @Param("lockedBefore") LocalDateTime lockedBefore,
            @Param("now") LocalDateTime now);
}
