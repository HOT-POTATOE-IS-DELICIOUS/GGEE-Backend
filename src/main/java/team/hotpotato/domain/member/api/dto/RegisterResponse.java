package team.hotpotato.domain.member.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RegisterResponse(
        @JsonProperty("indexing_job_id") String indexingJobId
) {
}
