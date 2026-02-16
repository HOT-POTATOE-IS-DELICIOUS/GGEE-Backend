package team.hotpotato.infrastructure.adapter.r2dbc.user;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import team.hotpotato.application.exception.EmailAlreadyExistsException;
import team.hotpotato.application.port.output.UserAppender;
import team.hotpotato.domain.user.User;
import team.hotpotato.infrastructure.entity.UserEntity;

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
