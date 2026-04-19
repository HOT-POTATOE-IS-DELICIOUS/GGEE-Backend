package team.hotpotato.domain.member.infrastructure.r2dbc.indexing;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import team.hotpotato.domain.member.domain.ProtectTargetIndexingOutbox;
import team.hotpotato.domain.member.domain.ProtectTargetIndexingOutboxStatus;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProtectTargetIndexingOutboxEntityMapper {

    public static ProtectTargetIndexingOutboxEntity toEntity(ProtectTargetIndexingOutbox outbox) {
        return ProtectTargetIndexingOutboxEntity.builder()
                .id(outbox.id())
                .protectTarget(outbox.protectTarget())
                .protectTargetInfo(outbox.protectTargetInfo())
                .status(outbox.status().name())
                .publishedAt(outbox.publishedAt())
                .build();
    }

    public static ProtectTargetIndexingOutbox toDomain(ProtectTargetIndexingOutboxEntity entity) {
        return new ProtectTargetIndexingOutbox(
                entity.getId(),
                entity.getProtectTarget(),
                entity.getProtectTargetInfo(),
                ProtectTargetIndexingOutboxStatus.valueOf(entity.getStatus()),
                entity.getPublishedAt()
        );
    }
}
