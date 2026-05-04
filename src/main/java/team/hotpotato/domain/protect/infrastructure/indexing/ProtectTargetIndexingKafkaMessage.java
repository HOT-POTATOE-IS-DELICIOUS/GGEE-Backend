package team.hotpotato.domain.protect.infrastructure.indexing;

import com.fasterxml.jackson.annotation.JsonProperty;

record ProtectTargetIndexingKafkaMessage(
        @JsonProperty("job_id") Long jobId,
        String keyword,
        @JsonProperty("protect_target_info") String protectTargetInfo
) {
}
