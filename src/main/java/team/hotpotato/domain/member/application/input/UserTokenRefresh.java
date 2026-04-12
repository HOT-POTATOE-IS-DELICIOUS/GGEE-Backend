package team.hotpotato.domain.member.application.input;

import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.application.usecase.refresh.RefreshCommand;
import team.hotpotato.domain.member.application.usecase.refresh.RefreshResult;

public interface UserTokenRefresh {
    Mono<RefreshResult> refresh(RefreshCommand command);
}
