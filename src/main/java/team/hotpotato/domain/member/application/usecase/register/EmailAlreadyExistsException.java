package team.hotpotato.domain.member.application.usecase.register;

import team.hotpotato.common.exception.BusinessBaseException;
import team.hotpotato.common.exception.ErrorCode;

public class EmailAlreadyExistsException extends BusinessBaseException {
    public static final BusinessBaseException EXCEPTION = new EmailAlreadyExistsException();

    private EmailAlreadyExistsException() {
        super(ErrorCode.EMAIL_ALREADY_EXISTS);
    }
}
