package team.hotpotato.support.advice;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.MethodNotAllowedException;
import reactor.core.publisher.Mono;
import team.hotpotato.common.exception.BusinessBaseException;
import team.hotpotato.common.exception.ErrorCode;
import team.hotpotato.support.logging.ExceptionLogger;

@RestControllerAdvice
@RequiredArgsConstructor
public class ApiControllerAdvice {
    private final ExceptionLogger exceptionLogger;
    private final ErrorCodeHttpStatusMapper errorCodeHttpStatusMapper;

    @ExceptionHandler(MethodNotAllowedException.class)
    protected Mono<ResponseEntity<String>> handle(MethodNotAllowedException e) {
        exceptionLogger.log(ErrorCode.METHOD_NOT_ALLOWED.message);

        return Mono.just(
                createErrorResponseEntity(ErrorCode.METHOD_NOT_ALLOWED)
        );
    }

    @ExceptionHandler(WebExchangeBindException.class)
    protected Mono<ResponseEntity<String>> handle(WebExchangeBindException e) {
        var fieldError = e.getFieldError();
        String errorMessage = fieldError != null ? fieldError.getDefaultMessage() : ErrorCode.INTERNAL_SERVER_ERROR.message;
        exceptionLogger.log(errorMessage);

        return Mono.just(ResponseEntity.badRequest().body(errorMessage));
    }

    @ExceptionHandler(BusinessBaseException.class)
    protected Mono<ResponseEntity<String>> handle(BusinessBaseException e) {
        exceptionLogger.log(e.getMessage());

        return Mono.just(
                createErrorResponseEntity(e.getErrorCode())
        );
    }

    @ExceptionHandler(Exception.class)
    protected Mono<ResponseEntity<String>> handle(Exception e) {
        exceptionLogger.log(e.getMessage());

        return Mono.just(createErrorResponseEntity(ErrorCode.INTERNAL_SERVER_ERROR));
    }


    private ResponseEntity<String> createErrorResponseEntity(ErrorCode errorCode) {
        return ResponseEntity
                .status(errorCodeHttpStatusMapper.toHttpStatus(errorCode))
                .body(errorCode.message);
    }
}
