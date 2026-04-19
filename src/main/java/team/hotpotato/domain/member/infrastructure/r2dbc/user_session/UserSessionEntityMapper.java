package team.hotpotato.domain.member.infrastructure.r2dbc.user_session;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import team.hotpotato.domain.member.domain.Session;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserSessionEntityMapper {

    public static UserSessionEntity toEntity(Session session) {
        return UserSessionEntity.builder()
                .id(session.id())
                .userId(session.userId())
                .sessionId(session.sessionId())
                .refreshToken(session.refreshToken())
                .expiresAt(session.expiresAt())
                .build();
    }

    public static Session toDomain(UserSessionEntity entity) {
        return new Session(
                entity.getId(),
                entity.getUserId(),
                entity.getSessionId(),
                entity.getRefreshToken(),
                entity.getExpiresAt()
        );
    }
}
