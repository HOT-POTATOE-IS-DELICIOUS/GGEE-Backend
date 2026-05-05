package team.hotpotato.domain.reaction.application.query.read;

import team.hotpotato.common.exception.BusinessBaseException;
import team.hotpotato.common.exception.ErrorCode;

public class NodeNewsServiceUnavailableException extends BusinessBaseException {
    public static final BusinessBaseException EXCEPTION = new NodeNewsServiceUnavailableException();

    private NodeNewsServiceUnavailableException() {
        super(ErrorCode.NEWS_SERVICE_UNAVAILABLE);
    }
}
