package team.hotpotato.domain.strategy.domain;

import java.time.LocalDateTime;

public record StrategyChatMessage(
        Long id,
        Long roomId,
        MessageRole role,
        String content,
        String intent,
        String refinedQuery,
        String metaJson,
        String aiMessageId,
        LocalDateTime createdAt
) {
}
