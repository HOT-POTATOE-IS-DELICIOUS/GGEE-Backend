package team.hotpotato.infrastructure.r2dbc.user_session;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import team.hotpotato.infrastructure.r2dbc.common.BaseEntity;

import java.time.LocalDateTime;

@Table("user_sessions")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSessionEntity extends BaseEntity {
    @Column("user_id")
    private Long userId;

    @Column("session_id")
    private String sessionId;

    @Column("refresh_token")
    private String refreshToken;

    @Column("expires_at")
    private LocalDateTime expiresAt;
}
