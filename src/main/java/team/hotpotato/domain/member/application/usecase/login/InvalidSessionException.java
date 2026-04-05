package team.hotpotato.domain.member.application.usecase.login;

import team.hotpotato.common.exception.BusinessBaseException;
import team.hotpotato.common.exception.ErrorCode;

public class InvalidSessionException extends BusinessBaseException {
    public static final BusinessBaseException EXCEPTION = new InvalidSessionException();

    private InvalidSessionException() {
        super(ErrorCode.INVALID_SESSION);
    }
}
