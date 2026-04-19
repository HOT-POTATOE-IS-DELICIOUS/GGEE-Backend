package team.hotpotato.domain.member.infrastructure.r2dbc.user;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import team.hotpotato.domain.member.application.usecase.register.EmailAlreadyExistsException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class UserRepositoryExceptionMapper {
    static Throwable mapToDomainExceptionIfNeeded(Throwable throwable) {
        if (throwable instanceof DataIntegrityViolationException
                || (throwable.getMessage() != null && throwable.getMessage().contains("Duplicate entry"))) {
            return EmailAlreadyExistsException.EXCEPTION;
        }
        return throwable;
    }
}
