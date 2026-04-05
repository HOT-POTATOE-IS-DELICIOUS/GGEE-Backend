package team.hotpotato.domain.member.application.usecase.login;

import team.hotpotato.common.exception.BusinessBaseException;
import team.hotpotato.common.exception.ErrorCode;

public class SessionExpiredException extends BusinessBaseException {
    public static final BusinessBaseException EXCEPTION = new SessionExpiredException();

    private SessionExpiredException() {
        super(ErrorCode.SESSION_EXPIRED);
    }
}
