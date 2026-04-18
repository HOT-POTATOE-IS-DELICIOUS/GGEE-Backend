package team.hotpotato.domain.member.application.usecase.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.application.dto.ProtectTargetIndexingPublishCommand;
import team.hotpotato.domain.member.application.output.ProtectTargetIndexingPublisher;
import team.hotpotato.domain.member.application.output.ProtectTargetIndexingOutboxRepository;

@Slf4j
@RequiredArgsConstructor
public class ProtectTargetIndexingOutboxDispatchUseCase {
    private final ProtectTargetIndexingOutboxRepository outboxRepository;
    private final ProtectTargetIndexingPublisher protectTargetIndexingPublisher;

    public Mono<Void> dispatchPending() {
        return outboxRepository.findPending()
                                .concatMap(outbox -> protectTargetIndexingPublisher.publish(
                                        new ProtectTargetIndexingPublishCommand(
                                                String.valueOf(outbox.id()),
                                                outbox.protectTarget(),
                                                outbox.protectTargetInfo()
                                        )
                                )
                                .doOnError(error -> log.error(
                                        "보호 대상 인덱싱 이벤트 발행 실패. outboxId={}, protectTarget={}",
                                        outbox.id(),
                                        outbox.protectTarget(),
                                        error
                                ))
                                .then(Mono.defer(() -> outboxRepository.markPublished(outbox.id())))
                                .doOnError(error -> log.error(
                                        "보호 대상 인덱싱 outbox markPublished 실패 (중복 발행 위험). outboxId={}",
                                        outbox.id(),
                                        error
                                ))
                                .onErrorResume(error -> Mono.empty())
                )
                .then();
    }
}
