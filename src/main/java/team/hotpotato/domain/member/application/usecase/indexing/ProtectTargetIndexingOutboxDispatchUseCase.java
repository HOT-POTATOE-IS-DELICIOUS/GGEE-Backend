package team.hotpotato.domain.member.application.usecase.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.application.event.ProtectTargetIndexingMessage;
import team.hotpotato.domain.member.application.output.ProtectTargetIndexingOutboxReader;
import team.hotpotato.domain.member.application.output.ProtectTargetIndexingOutboxUpdater;
import team.hotpotato.domain.member.application.output.ProtectTargetIndexingPublisher;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProtectTargetIndexingOutboxDispatchUseCase {
    private final ProtectTargetIndexingOutboxReader outboxReader;
    private final ProtectTargetIndexingPublisher protectTargetIndexingPublisher;
    private final ProtectTargetIndexingOutboxUpdater outboxUpdater;

    public Mono<Void> dispatchPending() {
        return outboxReader.findPending()
                .concatMap(outbox -> protectTargetIndexingPublisher.publish(
                                        new ProtectTargetIndexingMessage(outbox.protectTarget())
                                )
                                .then(Mono.defer(() -> outboxUpdater.markPublished(outbox.id())))
                                .doOnError(error -> log.error(
                                        "보호 대상 인덱싱 outbox 처리에 실패했습니다. outboxId={}, protectTarget={}",
                                        outbox.id(),
                                        outbox.protectTarget(),
                                        error
                                ))
                                .onErrorResume(error -> Mono.empty())
                )
                .then();
    }
}
