package team.hotpotato.domain.member.application.query.user;

import team.hotpotato.common.exception.BusinessBaseException;
import team.hotpotato.common.exception.ErrorCode;

public class UserNotFoundException extends BusinessBaseException {
    public static final BusinessBaseException EXCEPTION = new UserNotFoundException();

    private UserNotFoundException() {
        super(ErrorCode.USER_NOT_FOUND);
    }
}
