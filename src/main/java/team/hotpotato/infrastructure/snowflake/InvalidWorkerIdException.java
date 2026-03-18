package team.hotpotato.infrastructure.snowflake;

import team.hotpotato.common.exception.BusinessBaseException;
import team.hotpotato.common.exception.ErrorCode;

public class InvalidWorkerIdException extends BusinessBaseException {
    public static final BusinessBaseException EXCEPTION = new InvalidWorkerIdException();

    private InvalidWorkerIdException() {
        super(ErrorCode.INVALID_WORKER_ID);
    }
}
