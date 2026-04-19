package team.hotpotato.domain.audit.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuditHttpRequest(
        @JsonProperty("entity_name") String protectTarget,
        @JsonProperty("entity_info") String protectTargetInfo,
        String text
) {
}
