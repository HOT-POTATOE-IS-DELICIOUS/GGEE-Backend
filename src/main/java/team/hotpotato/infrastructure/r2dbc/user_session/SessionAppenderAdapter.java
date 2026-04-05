package team.hotpotato.infrastructure.r2dbc.user_session;

import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.application.output.SessionAppender;
import team.hotpotato.domain.member.domain.Session;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class SessionAppenderAdapter implements SessionAppender {
    private final R2dbcEntityTemplate template;

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
}
