package team.hotpotato.infrastructure.jwt;

import team.hotpotato.common.exception.BusinessBaseException;
import team.hotpotato.common.exception.ErrorCode;

public class ExpiredRefreshTokenException extends BusinessBaseException {
    public static final BusinessBaseException EXCEPTION = new ExpiredRefreshTokenException();

    private ExpiredRefreshTokenException() {
        super(ErrorCode.EXPIRED_REFRESH_TOKEN);
    }
}
