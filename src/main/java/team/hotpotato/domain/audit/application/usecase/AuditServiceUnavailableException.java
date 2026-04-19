package team.hotpotato.domain.audit.application.usecase;

import team.hotpotato.common.exception.BusinessBaseException;
import team.hotpotato.common.exception.ErrorCode;

public class AuditServiceUnavailableException extends BusinessBaseException {
    public static final BusinessBaseException EXCEPTION = new AuditServiceUnavailableException();

    private AuditServiceUnavailableException() {
        super(ErrorCode.AUDIT_SERVICE_UNAVAILABLE);
    }
}
