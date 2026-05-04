package team.hotpotato.domain.protect.application.usecase.query;

import team.hotpotato.common.exception.BusinessBaseException;
import team.hotpotato.common.exception.ErrorCode;

public class ProtectNotFoundException extends BusinessBaseException {
    public static final BusinessBaseException EXCEPTION = new ProtectNotFoundException();

    private ProtectNotFoundException() {
        super(ErrorCode.PROTECT_NOT_FOUND);
    }
}
