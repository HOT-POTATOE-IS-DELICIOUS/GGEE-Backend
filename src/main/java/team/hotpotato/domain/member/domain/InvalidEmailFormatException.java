package team.hotpotato.domain.member.domain;

import team.hotpotato.common.exception.BusinessBaseException;
import team.hotpotato.common.exception.ErrorCode;

public class InvalidEmailFormatException extends BusinessBaseException {
    public static final BusinessBaseException EXCEPTION = new InvalidEmailFormatException();

    private InvalidEmailFormatException() {
        super(ErrorCode.INVALID_EMAIL_FORMAT);
    }
}
