package team.hotpotato.domain.member.application.usecase.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.application.event.ProtectTargetIndexingMessage;
import team.hotpotato.domain.member.application.output.ProtectTargetIndexingPublisher;
import team.hotpotato.domain.member.application.output.ProtectTargetIndexingOutboxRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProtectTargetIndexingOutboxDispatchUseCase {
    private final ProtectTargetIndexingOutboxRepository outboxRepository;
    private final ProtectTargetIndexingPublisher protectTargetIndexingPublisher;

    public Mono<Void> dispatchPending() {
        return outboxRepository.findPending()
                .concatMap(outbox -> protectTargetIndexingPublisher.publish(
                                        new ProtectTargetIndexingMessage(
                                                String.valueOf(outbox.id()),
                                                outbox.protectTarget()
                                        )
                                )
                                .then(Mono.defer(() -> outboxRepository.markPublished(outbox.id())))
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
