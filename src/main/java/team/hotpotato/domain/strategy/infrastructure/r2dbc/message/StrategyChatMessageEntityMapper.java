package team.hotpotato.domain.strategy.infrastructure.r2dbc.message;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import team.hotpotato.domain.strategy.domain.MessageRole;
import team.hotpotato.domain.strategy.domain.StrategyChatMessage;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StrategyChatMessageEntityMapper {

    public static StrategyChatMessageEntity toEntity(StrategyChatMessage message) {
        return StrategyChatMessageEntity.builder()
                .id(message.id())
                .roomId(message.roomId())
                .role(message.role().name())
                .content(message.content())
                .intent(message.intent())
                .refinedQuery(message.refinedQuery())
                .metaJson(message.metaJson())
                .aiMessageId(message.aiMessageId())
                .build();
    }

    public static StrategyChatMessage toDomain(StrategyChatMessageEntity entity) {
        return new StrategyChatMessage(
                entity.getId(),
                entity.getRoomId(),
                MessageRole.valueOf(entity.getRole()),
                entity.getContent(),
                entity.getIntent(),
                entity.getRefinedQuery(),
                entity.getMetaJson(),
                entity.getAiMessageId(),
                entity.getCreatedAt()
        );
    }
}
