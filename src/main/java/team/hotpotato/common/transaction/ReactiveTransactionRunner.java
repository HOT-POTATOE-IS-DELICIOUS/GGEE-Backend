package team.hotpotato.common.transaction;

import reactor.core.publisher.Mono;

public interface ReactiveTransactionRunner {

    <T> Mono<T> transactional(Mono<T> mono);
}
