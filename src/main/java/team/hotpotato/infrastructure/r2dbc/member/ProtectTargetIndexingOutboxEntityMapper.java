package team.hotpotato.infrastructure.r2dbc.member;

import lombok.NoArgsConstructor;
import team.hotpotato.domain.member.domain.ProtectTargetIndexingOutbox;
import team.hotpotato.domain.member.domain.ProtectTargetIndexingOutboxStatus;

@NoArgsConstructor
public final class ProtectTargetIndexingOutboxEntityMapper {

    public static ProtectTargetIndexingOutboxEntity toEntity(ProtectTargetIndexingOutbox outbox) {
        return ProtectTargetIndexingOutboxEntity.builder()
                .id(outbox.id())
                .protectTarget(outbox.protectTarget())
                .status(outbox.status().name())
                .publishedAt(outbox.publishedAt())
                .build();
    }

    public static ProtectTargetIndexingOutbox toDomain(ProtectTargetIndexingOutboxEntity entity) {
        return new ProtectTargetIndexingOutbox(
                entity.getId(),
                entity.getProtectTarget(),
                ProtectTargetIndexingOutboxStatus.valueOf(entity.getStatus()),
                entity.getPublishedAt()
        );
    }
}
