package team.hotpotato.support.advice;

import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
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

    @ExceptionHandler(HandlerMethodValidationException.class)
    protected Mono<ResponseEntity<String>> handle(HandlerMethodValidationException e) {
        String errorMessage = e.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .findFirst()
                .orElse(ErrorCode.INTERNAL_SERVER_ERROR.message);
        exceptionLogger.log(errorMessage);

        return Mono.just(ResponseEntity.badRequest().body(errorMessage));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    protected Mono<ResponseEntity<String>> handle(ConstraintViolationException e) {
        String errorMessage = e.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .findFirst()
                .orElse(ErrorCode.INTERNAL_SERVER_ERROR.message);
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
