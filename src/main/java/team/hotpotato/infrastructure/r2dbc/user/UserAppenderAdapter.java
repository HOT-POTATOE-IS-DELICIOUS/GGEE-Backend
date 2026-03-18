package team.hotpotato.infrastructure.r2dbc.user;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.application.persistence.UserAppender;
import team.hotpotato.domain.member.application.usecase.EmailAlreadyExistsException;
import team.hotpotato.domain.member.domain.User;

@Repository
@RequiredArgsConstructor
public class UserAppenderAdapter implements UserAppender {
    private final R2dbcEntityTemplate template;

    @Override
    public Mono<User> save(User user) {
        return template.insert(UserEntity.class)
                .using(UserEntityMapper.toEntity(user))
                .map(UserEntityMapper::toDomain)
                .onErrorMap(this::mapToDomainExceptionIfNeeded);
    }

    private Throwable mapToDomainExceptionIfNeeded(Throwable throwable) {
        if (throwable instanceof DataIntegrityViolationException
                || (throwable.getMessage() != null && throwable.getMessage().contains("Duplicate entry"))) {
            return EmailAlreadyExistsException.EXCEPTION;
        }
        return throwable;
    }
}
