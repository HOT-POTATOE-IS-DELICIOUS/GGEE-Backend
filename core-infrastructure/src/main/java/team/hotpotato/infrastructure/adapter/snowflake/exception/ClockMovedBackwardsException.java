package team.hotpotato.infrastructure.adapter.snowflake.exception;

import team.hotpotato.common.exception.BusinessBaseException;
import team.hotpotato.common.exception.ErrorCode;

public class ClockMovedBackwardsException extends BusinessBaseException {
    public static final ClockMovedBackwardsException EXCEPTION = new ClockMovedBackwardsException();

    private ClockMovedBackwardsException() {
        super(ErrorCode.CLOCK_MOVED_BACKWARDS);
    }
}
