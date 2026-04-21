package team.hotpotato.domain.strategy.api.dto;

import team.hotpotato.domain.strategy.domain.MessageRole;

import java.time.LocalDateTime;

public record MessageResponse(
        Long messageId,
        MessageRole role,
        String content,
        String intent,
        String refinedQuery,
        String metaJson,
        LocalDateTime createdAt
) {
}
