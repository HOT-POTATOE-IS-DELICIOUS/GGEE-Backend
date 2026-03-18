package team.hotpotato.domain.member.application.usecase;

import team.hotpotato.common.exception.BusinessBaseException;
import team.hotpotato.common.exception.ErrorCode;

public class InvalidEmailOrPasswordException extends BusinessBaseException {
    public static final BusinessBaseException EXCEPTION = new InvalidEmailOrPasswordException();

    private InvalidEmailOrPasswordException() {
        super(ErrorCode.INVALID_EMAIL_OR_PASSWORD);
    }
}
