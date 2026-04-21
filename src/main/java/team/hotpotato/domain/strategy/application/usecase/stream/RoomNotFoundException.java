package team.hotpotato.domain.strategy.application.usecase.stream;

import team.hotpotato.common.exception.BusinessBaseException;
import team.hotpotato.common.exception.ErrorCode;

public class RoomNotFoundException extends BusinessBaseException {
    public static final BusinessBaseException EXCEPTION = new RoomNotFoundException();

    private RoomNotFoundException() {
        super(ErrorCode.STRATEGY_ROOM_NOT_FOUND);
    }
}
