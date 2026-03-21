package team.hotpotato.infrastructure.r2dbc.common;

import lombok.AccessLevel;
import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;

import java.time.LocalDateTime;

@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BaseEntity {
    @Id
    @Column("id")
    private Long id;

    @Column("updatedAt")
    private LocalDateTime updatedAt;

    @Column("createdAt")
    private LocalDateTime createdAt;

    @Column("deleted")
    private Boolean deleted;

    @Column("deleted_at")
    private LocalDateTime deletedAt;
}
