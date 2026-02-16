package team.hotpotato.infrastructure.adapter.jwt.exception;

import team.hotpotato.common.exception.BusinessBaseException;
import team.hotpotato.common.exception.ErrorCode;

public class InvalidTokenTypeException extends BusinessBaseException {
    public static final BusinessBaseException EXCEPTION = new InvalidTokenTypeException();

    private InvalidTokenTypeException() {
        super(ErrorCode.INVALID_TOKEN_TYPE);
    }
}
