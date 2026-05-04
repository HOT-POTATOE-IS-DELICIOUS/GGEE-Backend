package team.hotpotato.domain.protect.application.input;

import reactor.core.publisher.Mono;
import team.hotpotato.domain.protect.application.usecase.indexing.IndexProtectCommand;
import team.hotpotato.domain.protect.application.usecase.indexing.IndexProtectResult;

public interface IndexProtect {
    Mono<IndexProtectResult> index(IndexProtectCommand command);
}
