package team.hotpotato.infrastructure.common;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import team.hotpotato.common.transaction.ReactiveTransactionRunner;

@Component
@RequiredArgsConstructor
public class SpringReactiveTransactionRunner implements ReactiveTransactionRunner {

    private final TransactionalOperator transactionalOperator;

    @Override
    public <T> Mono<T> transactional(Mono<T> mono) {
        return transactionalOperator.transactional(mono);
    }
}
