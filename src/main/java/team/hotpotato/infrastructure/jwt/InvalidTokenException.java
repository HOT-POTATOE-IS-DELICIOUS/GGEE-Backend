package team.hotpotato.infrastructure.jwt;

import team.hotpotato.common.exception.BusinessBaseException;
import team.hotpotato.common.exception.ErrorCode;

public class InvalidTokenException extends BusinessBaseException {
    public static final BusinessBaseException EXCEPTION = new InvalidTokenException();

    private InvalidTokenException() {
        super(ErrorCode.INVALID_TOKEN);
    }
}
