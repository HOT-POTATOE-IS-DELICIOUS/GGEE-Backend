package team.hotpotato.domain.audit.infrastructure.r2dbc;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import team.hotpotato.infrastructure.r2dbc.common.BaseEntity;

@Table("audits")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditEntity extends BaseEntity {
    @Column("user_id")
    private Long userId;

    @Column("protect_target")
    private String protectTarget;

    @Column("protect_target_info")
    private String protectTargetInfo;

    @Column("text")
    private String text;

    @Column("message_id")
    private String messageId;

    @Column("reviews_json")
    private String reviewsJson;
}
