package team.hotpotato.domain.audit.infrastructure.r2dbc;

import lombok.NoArgsConstructor;
import team.hotpotato.domain.audit.domain.Audit;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class AuditEntityMapper {

    public static AuditEntity toEntity(Audit audit, String reviewsJson) {
        return AuditEntity.builder()
                .id(audit.auditId())
                .userId(audit.userId())
                .protectTarget(audit.protectTarget())
                .protectTargetInfo(audit.protectTargetInfo())
                .text(audit.text())
                .messageId(audit.messageId())
                .reviewsJson(reviewsJson)
                .build();
    }
}
