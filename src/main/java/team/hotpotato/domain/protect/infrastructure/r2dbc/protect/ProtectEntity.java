package team.hotpotato.domain.protect.infrastructure.r2dbc.protect;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import team.hotpotato.infrastructure.r2dbc.common.BaseEntity;

@Table("protects")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProtectEntity extends BaseEntity {
    @Column("user_id")
    private Long userId;

    @Column("target")
    private String target;

    @Column("info")
    private String info;
}
