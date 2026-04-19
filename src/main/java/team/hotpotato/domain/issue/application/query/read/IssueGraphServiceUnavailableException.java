package team.hotpotato.domain.issue.application.query.read;

import team.hotpotato.common.exception.BusinessBaseException;
import team.hotpotato.common.exception.ErrorCode;

public class IssueGraphServiceUnavailableException extends BusinessBaseException {
    public static final BusinessBaseException EXCEPTION = new IssueGraphServiceUnavailableException();

    private IssueGraphServiceUnavailableException() {
        super(ErrorCode.ISSUE_GRAPH_SERVICE_UNAVAILABLE);
    }
}
