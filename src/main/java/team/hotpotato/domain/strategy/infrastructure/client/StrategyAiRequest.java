package team.hotpotato.domain.strategy.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StrategyAiRequest(
        String message,
        @JsonProperty("entity_name") String entityName,
        @JsonProperty("entity_info") String entityInfo
) {
}
