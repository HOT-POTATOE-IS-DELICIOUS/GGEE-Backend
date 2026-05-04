package team.hotpotato.domain.protect.application.usecase.indexing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import team.hotpotato.common.identity.IdGenerator;
import team.hotpotato.domain.protect.application.output.ProtectRepository;
import team.hotpotato.domain.protect.application.output.ProtectTargetIndexingOutboxRepository;
import team.hotpotato.domain.protect.domain.ProtectTargetIndexingOutbox;
import team.hotpotato.domain.protect.domain.ProtectTargetIndexingOutboxStatus;
import team.hotpotato.domain.protect.domain.ProtectTargetSnapshot;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("보호 대상 갱신 스케줄링 유스케이스 단위 테스트")
class ProtectTargetRefreshScheduleUseCaseTest {

    @Mock
    private ProtectRepository protectRepository;

    @Mock
    private ProtectTargetIndexingOutboxRepository outboxRepository;

    @Mock
    private IdGenerator idGenerator;

    private ProtectTargetRefreshScheduleUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ProtectTargetRefreshScheduleUseCase(protectRepository, outboxRepository, idGenerator);
    }

    @Test
    @DisplayName("고유한 보호 대상마다 PENDING outbox 행을 1건씩 적재한다")
    void enqueuesOnePendingOutboxPerDistinctSnapshot() {
        ProtectTargetSnapshot brandSnapshot = new ProtectTargetSnapshot("brand", "브랜드 공식몰");
        ProtectTargetSnapshot personSnapshot = new ProtectTargetSnapshot("celeb", "공식 SNS");
        when(protectRepository.findActiveDistinctProtectTargets())
                .thenReturn(Flux.just(brandSnapshot, personSnapshot));

        AtomicLong nextId = new AtomicLong(1000L);
        when(idGenerator.generateId()).thenAnswer(invocation -> nextId.getAndIncrement());
        when(outboxRepository.save(any(ProtectTargetIndexingOutbox.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.scheduleAll())
                .expectNext(2L)
                .verifyComplete();

        ArgumentCaptor<ProtectTargetIndexingOutbox> captor = ArgumentCaptor.forClass(ProtectTargetIndexingOutbox.class);
        verify(outboxRepository, times(2)).save(captor.capture());

        List<ProtectTargetIndexingOutbox> savedOutboxes = captor.getAllValues();
        assertThat(savedOutboxes).extracting(ProtectTargetIndexingOutbox::status)
                .containsOnly(ProtectTargetIndexingOutboxStatus.PENDING);
        assertThat(savedOutboxes).extracting(ProtectTargetIndexingOutbox::publishedAt)
                .containsOnlyNulls();
        assertThat(savedOutboxes).extracting(ProtectTargetIndexingOutbox::protectTarget)
                .containsExactly("brand", "celeb");
        assertThat(savedOutboxes).extracting(ProtectTargetIndexingOutbox::protectTargetInfo)
                .containsExactly("브랜드 공식몰", "공식 SNS");
        assertThat(savedOutboxes).extracting(ProtectTargetIndexingOutbox::id)
                .doesNotContainNull()
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("활성 사용자가 없으면 outbox 적재 없이 0을 반환한다")
    void returnsZeroAndSkipsSaveWhenNoActiveProtectTargets() {
        when(protectRepository.findActiveDistinctProtectTargets()).thenReturn(Flux.empty());

        StepVerifier.create(useCase.scheduleAll())
                .expectNext(0L)
                .verifyComplete();

        verify(outboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("일부 행 적재가 실패해도 나머지 행은 적재되며 성공 건수만 반환한다")
    void continuesOnPerRowFailureAndReturnsSuccessCountOnly() {
        ProtectTargetSnapshot first = new ProtectTargetSnapshot("a", "info-a");
        ProtectTargetSnapshot second = new ProtectTargetSnapshot("b", "info-b");
        ProtectTargetSnapshot third = new ProtectTargetSnapshot("c", "info-c");
        when(protectRepository.findActiveDistinctProtectTargets())
                .thenReturn(Flux.just(first, second, third));

        AtomicLong nextId = new AtomicLong(1L);
        when(idGenerator.generateId()).thenAnswer(invocation -> nextId.getAndIncrement());
        when(outboxRepository.save(any(ProtectTargetIndexingOutbox.class)))
                .thenAnswer(invocation -> {
                    ProtectTargetIndexingOutbox outbox = invocation.getArgument(0);
                    if ("b".equals(outbox.protectTarget())) {
                        return Mono.error(new RuntimeException("save failed"));
                    }
                    return Mono.just(outbox);
                });

        StepVerifier.create(useCase.scheduleAll())
                .expectNext(2L)
                .verifyComplete();

        verify(outboxRepository, times(3)).save(any(ProtectTargetIndexingOutbox.class));
    }
}
