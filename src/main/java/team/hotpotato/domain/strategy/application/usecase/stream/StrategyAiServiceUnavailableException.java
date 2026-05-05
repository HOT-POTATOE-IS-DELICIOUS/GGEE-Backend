package team.hotpotato.domain.strategy.application.usecase.stream;

import team.hotpotato.common.exception.BusinessBaseException;
import team.hotpotato.common.exception.ErrorCode;

public class StrategyAiServiceUnavailableException extends BusinessBaseException {
    public static final BusinessBaseException EXCEPTION = new StrategyAiServiceUnavailableException();

    private StrategyAiServiceUnavailableException() {
        super(ErrorCode.STRATEGY_AI_SERVICE_UNAVAILABLE);
    }
}
