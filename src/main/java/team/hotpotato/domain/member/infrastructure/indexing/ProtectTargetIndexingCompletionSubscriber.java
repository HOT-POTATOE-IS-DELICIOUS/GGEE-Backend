package team.hotpotato.domain.member.infrastructure.indexing;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.application.output.ProtectTargetIndexingOutboxRepository;
import team.hotpotato.domain.reaction.application.output.IndexingJobCompletionEvents;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProtectTargetIndexingCompletionSubscriber {

    private final IndexingJobCompletionEvents completionEvents;
    private final ProtectTargetIndexingOutboxRepository outboxRepository;

    @PostConstruct
    public void subscribe() {
        completionEvents.completions()
                .flatMap(jobId -> {
                    try {
                        Long outboxId = Long.parseLong(jobId);
                        return outboxRepository.markCompleted(outboxId)
                                .doOnSuccess(v -> log.info("인덱싱 완료 outbox 업데이트. outboxId={}", outboxId))
                                .onErrorResume(e -> {
                                    log.error("인덱싱 완료 outbox 업데이트 실패. outboxId={}", outboxId, e);
                                    return Mono.empty();
                                });
                    } catch (NumberFormatException e) {
                        log.warn("유효하지 않은 jobId 형식. jobId={}", jobId);
                        return Mono.empty();
                    }
                })
                .subscribe();
    }
}
