package team.hotpotato.domain.protect.application.usecase.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.protect.application.dto.ProtectTargetIndexingPublishCommand;
import team.hotpotato.domain.protect.application.output.ProtectTargetIndexingPublisher;
import team.hotpotato.domain.protect.application.output.ProtectTargetIndexingOutboxRepository;
import team.hotpotato.domain.protect.domain.ProtectTargetIndexingOutbox;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProtectTargetIndexingOutboxDispatchUseCase {
    private final ProtectTargetIndexingOutboxRepository outboxRepository;
    private final ProtectTargetIndexingPublisher protectTargetIndexingPublisher;

    public Mono<Void> dispatchPending() {
        return outboxRepository.findPending()
                .concatMap(outbox -> outboxRepository.claim(outbox.id())
                        .flatMap(affected -> {
                            if (affected == 0) {
                                log.debug("보호 대상 인덱싱 outbox claim 경합 패배, 건너뜀. outboxId={}", outbox.id());
                                return Mono.empty();
                            }
                            return publish(outbox);
                        })
                )
                .then();
    }

    private Mono<Void> publish(ProtectTargetIndexingOutbox outbox) {
        return protectTargetIndexingPublisher.publish(
                        new ProtectTargetIndexingPublishCommand(
                                outbox.id(),
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
                .onErrorResume(publishError -> outboxRepository.rollbackToPending(outbox.id())
                        .doOnError(rollbackError -> log.error(
                                "보호 대상 인덱싱 outbox PENDING 복구 실패. outboxId={}",
                                outbox.id(),
                                rollbackError
                        ))
                        .onErrorResume(rollbackError -> Mono.empty())
                        .then(Mono.empty())
                );
    }
}
