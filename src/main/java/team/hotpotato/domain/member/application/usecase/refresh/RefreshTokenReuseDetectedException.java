package team.hotpotato.domain.member.application.usecase.refresh;

import team.hotpotato.common.exception.BusinessBaseException;
import team.hotpotato.common.exception.ErrorCode;

public class RefreshTokenReuseDetectedException extends BusinessBaseException {
    public static final BusinessBaseException EXCEPTION = new RefreshTokenReuseDetectedException();

    private RefreshTokenReuseDetectedException() {
        super(ErrorCode.REFRESH_TOKEN_REUSED);
    }
}
