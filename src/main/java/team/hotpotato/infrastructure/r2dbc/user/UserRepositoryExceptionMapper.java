package team.hotpotato.infrastructure.r2dbc.user;

import org.springframework.dao.DataIntegrityViolationException;
import team.hotpotato.domain.member.application.usecase.register.EmailAlreadyExistsException;

final class UserRepositoryExceptionMapper {
    private UserRepositoryExceptionMapper() {
    }

    static Throwable mapToDomainExceptionIfNeeded(Throwable throwable) {
        if (throwable instanceof DataIntegrityViolationException
                || (throwable.getMessage() != null && throwable.getMessage().contains("Duplicate entry"))) {
            return EmailAlreadyExistsException.EXCEPTION;
        }
        return throwable;
    }
}
