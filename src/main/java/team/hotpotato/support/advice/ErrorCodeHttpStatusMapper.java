package team.hotpotato.support.advice;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import team.hotpotato.common.exception.ErrorCode;

@Component
public class ErrorCodeHttpStatusMapper {

    public HttpStatus toHttpStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            case EXPIRED_TOKEN, EXPIRED_REFRESH_TOKEN, INVALID_EMAIL_OR_PASSWORD, SESSION_EXPIRED -> HttpStatus.UNAUTHORIZED;
            case INVALID_TOKEN, INVALID_TOKEN_TYPE, INVALID_EMAIL_FORMAT, INVALID_SESSION -> HttpStatus.BAD_REQUEST;
            case EMAIL_ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case USER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ISSUE_GRAPH_SERVICE_UNAVAILABLE, AUDIT_SERVICE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case METHOD_NOT_ALLOWED -> HttpStatus.METHOD_NOT_ALLOWED;
            case INTERNAL_SERVER_ERROR, CLOCK_MOVED_BACKWARDS, INVALID_WORKER_ID -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
