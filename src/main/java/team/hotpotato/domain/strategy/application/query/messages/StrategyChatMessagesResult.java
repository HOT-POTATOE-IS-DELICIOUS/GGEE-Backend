package team.hotpotato.domain.strategy.application.query.messages;

import team.hotpotato.domain.strategy.domain.MessageRole;

import java.time.LocalDateTime;

public record StrategyChatMessagesResult(
        Long messageId,
        MessageRole role,
        String content,
        String intent,
        String refinedQuery,
        String metaJson,
        LocalDateTime createdAt
) {
}
