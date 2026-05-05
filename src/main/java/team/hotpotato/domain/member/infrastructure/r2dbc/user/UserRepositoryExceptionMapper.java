package team.hotpotato.domain.member.infrastructure.r2dbc.user;

import io.r2dbc.spi.R2dbcDataIntegrityViolationException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import team.hotpotato.domain.member.application.usecase.register.EmailAlreadyExistsException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class UserRepositoryExceptionMapper {
    private static final String POSTGRESQL_UNIQUE_VIOLATION = "23505";

    static Throwable mapToDomainExceptionIfNeeded(Throwable throwable) {
        if (throwable instanceof DataIntegrityViolationException
                && throwable.getCause() instanceof R2dbcDataIntegrityViolationException r2dbcEx
                && POSTGRESQL_UNIQUE_VIOLATION.equals(r2dbcEx.getSqlState())) {
            return EmailAlreadyExistsException.EXCEPTION;
        }
        return throwable;
    }
}
