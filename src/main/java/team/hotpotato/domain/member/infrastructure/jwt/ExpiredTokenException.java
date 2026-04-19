package team.hotpotato.domain.member.infrastructure.jwt;

import team.hotpotato.common.exception.BusinessBaseException;
import team.hotpotato.common.exception.ErrorCode;

public class ExpiredTokenException extends BusinessBaseException {
    public static final BusinessBaseException EXCEPTION = new ExpiredTokenException();

    private ExpiredTokenException() {
        super(ErrorCode.EXPIRED_TOKEN);
    }
}
