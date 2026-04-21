package team.hotpotato.domain.strategy.infrastructure.client;

public record StrategyAiRequest(
        String message,
        String entityName,
        String entityInfo
) {
}
