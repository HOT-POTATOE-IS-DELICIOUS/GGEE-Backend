package team.hotpotato.infrastructure.r2dbc.user_session;

import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.application.output.SessionRepository;
import team.hotpotato.domain.member.domain.Session;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class SessionRepositoryAdapter implements SessionRepository {
    private final R2dbcEntityTemplate template;

    @Override
    public Mono<Session> findActiveByUserId(Long userId) {
        return template.selectOne(
                        Query.query(
                                Criteria.where("user_id").is(userId)
                                        .and("deleted").is(false)
                                        .and("expires_at").greaterThan(LocalDateTime.now())
                        ),
                        UserSessionEntity.class
                )
                .map(UserSessionEntityMapper::toDomain);
    }

    @Override
    public Mono<Session> findBySessionId(String sessionId) {
        return template.selectOne(
                        Query.query(
                                Criteria.where("session_id").is(sessionId)
                                        .and("deleted").is(false)
                        ),
                        UserSessionEntity.class
                )
                .map(UserSessionEntityMapper::toDomain);
    }

    @Override
    public Mono<Session> save(Session session) {
        return template.insert(UserSessionEntity.class)
                .using(UserSessionEntityMapper.toEntity(session))
                .map(UserSessionEntityMapper::toDomain);
    }

    @Override
    public Mono<Void> invalidateByUserId(Long userId) {
        return template.update(
                        Query.query(Criteria.where("user_id").is(userId)
                                .and("deleted").is(false)),
                        Update.update("deleted", true)
                                .set("deleted_at", LocalDateTime.now()),
                        UserSessionEntity.class
                )
                .then();
    }

    @Override
    public Mono<Void> updateRefreshToken(String sessionId, String newRefreshToken, LocalDateTime newExpiresAt) {
        return template.update(
                        Query.query(Criteria.where("session_id").is(sessionId)
                                .and("deleted").is(false)),
                        Update.update("refresh_token", newRefreshToken)
                                .set("expires_at", newExpiresAt),
                        UserSessionEntity.class
                )
                .then();
    }
}
