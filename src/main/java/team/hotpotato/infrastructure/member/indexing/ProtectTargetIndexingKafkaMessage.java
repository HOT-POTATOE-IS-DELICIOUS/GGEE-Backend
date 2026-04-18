package team.hotpotato.infrastructure.member.indexing;

import com.fasterxml.jackson.annotation.JsonProperty;

record ProtectTargetIndexingKafkaMessage(
        @JsonProperty("job_id")
        String jobId,
        String keyword
) {
}
