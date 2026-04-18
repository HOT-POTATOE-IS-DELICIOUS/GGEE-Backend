package team.hotpotato.infrastructure.r2dbc.member;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import team.hotpotato.infrastructure.r2dbc.common.BaseEntity;

import java.time.LocalDateTime;

@Table("protect_target_indexing_outbox")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProtectTargetIndexingOutboxEntity extends BaseEntity {
    @Column("protect_target")
    private String protectTarget;

    @Column("protect_target_info")
    private String protectTargetInfo;

    @Column("status")
    private String status;

    @Column("published_at")
    private LocalDateTime publishedAt;
}
