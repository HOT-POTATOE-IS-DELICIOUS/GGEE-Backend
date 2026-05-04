package team.hotpotato.domain.protect.application.usecase.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import team.hotpotato.common.identity.IdGenerator;
import team.hotpotato.domain.protect.application.input.ProtectTargetRefreshSchedule;
import team.hotpotato.domain.protect.application.output.ProtectRepository;
import team.hotpotato.domain.protect.application.output.ProtectTargetIndexingOutboxRepository;
import team.hotpotato.domain.protect.domain.ProtectTargetIndexingOutbox;
import team.hotpotato.domain.protect.domain.ProtectTargetIndexingOutboxStatus;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProtectTargetRefreshScheduleUseCase implements ProtectTargetRefreshSchedule {
    private final ProtectRepository protectRepository;
    private final ProtectTargetIndexingOutboxRepository outboxRepository;
    private final IdGenerator idGenerator;

    @Override
    public Mono<Long> scheduleAll() {
        return protectRepository.findActiveDistinctProtectTargets()
                .concatMap(snapshot -> outboxRepository.save(new ProtectTargetIndexingOutbox(
                                idGenerator.generateId(),
                                snapshot.protectTarget(),
                                snapshot.protectTargetInfo(),
                                ProtectTargetIndexingOutboxStatus.PENDING,
                                null
                        ))
                        .doOnError(error -> log.error(
                                "보호 대상 갱신 outbox 적재 실패. protectTarget={}",
                                snapshot.protectTarget(),
                                error
                        ))
                        .onErrorResume(error -> Mono.empty())
                )
                .count()
                .doOnSuccess(count -> {
                    if (count == 0) {
                        log.warn("보호 대상 갱신: 적재된 outbox가 0건. 활성 사용자 데이터 점검 필요");
                    } else {
                        log.info("보호 대상 갱신 outbox {}건 적재 완료", count);
                    }
                });
    }
}
