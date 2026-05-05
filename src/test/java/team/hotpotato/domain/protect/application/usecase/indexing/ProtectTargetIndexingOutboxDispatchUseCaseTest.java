package team.hotpotato.domain.protect.application.usecase.indexing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import team.hotpotato.domain.protect.application.dto.ProtectTargetIndexingPublishCommand;
import team.hotpotato.domain.protect.application.output.ProtectTargetIndexingOutboxRepository;
import team.hotpotato.domain.protect.application.output.ProtectTargetIndexingPublisher;
import team.hotpotato.domain.protect.application.usecase.indexing.ProtectTargetIndexingOutboxDispatchUseCase;
import team.hotpotato.domain.protect.domain.ProtectTargetIndexingOutbox;
import team.hotpotato.domain.protect.domain.ProtectTargetIndexingOutboxStatus;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("보호 대상 인덱싱 outbox dispatcher 단위 테스트")
class ProtectTargetIndexingOutboxDispatchUseCaseTest {

    @Mock
    private ProtectTargetIndexingOutboxRepository outboxRepository;

    @Mock
    private ProtectTargetIndexingPublisher publisher;

    @Test
    @DisplayName("pending outbox를 발행하고 published 처리한다")
    void dispatchPublishesAndMarksPublished() {
        ProtectTargetIndexingOutboxDispatchUseCase useCase =
                new ProtectTargetIndexingOutboxDispatchUseCase(outboxRepository, publisher);
        ProtectTargetIndexingOutbox outbox = new ProtectTargetIndexingOutbox(
                1L,
                "brand",
                "브랜드 공식몰",
                ProtectTargetIndexingOutboxStatus.PENDING,
                null
        );

        when(outboxRepository.findPending()).thenReturn(Flux.just(outbox));
        when(outboxRepository.claim(1L)).thenReturn(Mono.just(1L));
        when(publisher.publish(any())).thenReturn(Mono.empty());
        when(outboxRepository.markPublished(1L)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.dispatchPending())
                .verifyComplete();

        ArgumentCaptor<ProtectTargetIndexingPublishCommand> messageCaptor =
                ArgumentCaptor.forClass(ProtectTargetIndexingPublishCommand.class);
        verify(outboxRepository).findPending();
        verify(outboxRepository).claim(1L);
        verify(publisher).publish(messageCaptor.capture());
        verify(outboxRepository).markPublished(1L);
        ProtectTargetIndexingPublishCommand publishedMessage = messageCaptor.getValue();
        verifyNoMoreInteractions(outboxRepository, publisher);
        org.junit.jupiter.api.Assertions.assertEquals(1L, publishedMessage.jobId());
        org.junit.jupiter.api.Assertions.assertEquals("brand", publishedMessage.keyword());
        org.junit.jupiter.api.Assertions.assertEquals("브랜드 공식몰", publishedMessage.protectTargetInfo());
    }

    @Test
    @DisplayName("발행 실패 outbox는 PENDING으로 복구된다")
    void dispatchRollbacksToPendingWhenPublishFails() {
        ProtectTargetIndexingOutboxDispatchUseCase useCase =
                new ProtectTargetIndexingOutboxDispatchUseCase(outboxRepository, publisher);
        ProtectTargetIndexingOutbox outbox = new ProtectTargetIndexingOutbox(
                1L,
                "brand",
                "브랜드 공식몰",
                ProtectTargetIndexingOutboxStatus.PENDING,
                null
        );

        when(outboxRepository.findPending()).thenReturn(Flux.just(outbox));
        when(outboxRepository.claim(1L)).thenReturn(Mono.just(1L));
        when(publisher.publish(any())).thenReturn(Mono.error(new RuntimeException("publish failed")));
        when(outboxRepository.rollbackToPending(1L)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.dispatchPending())
                .verifyComplete();

        verify(outboxRepository).findPending();
        verify(outboxRepository).claim(1L);
        verify(publisher).publish(any());
        verify(outboxRepository, never()).markPublished(anyLong());
        verify(outboxRepository).rollbackToPending(1L);
        verifyNoMoreInteractions(outboxRepository, publisher);
    }

    @Test
    @DisplayName("이미 발행된 outbox가 없으면 종료한다")
    void dispatchCompletesWhenNoPendingOutboxExists() {
        ProtectTargetIndexingOutboxDispatchUseCase useCase =
                new ProtectTargetIndexingOutboxDispatchUseCase(outboxRepository, publisher);

        when(outboxRepository.findPending()).thenReturn(Flux.empty());

        StepVerifier.create(useCase.dispatchPending())
                .verifyComplete();

        verify(outboxRepository).findPending();
        verifyNoInteractions(publisher);
        verify(outboxRepository, never()).markPublished(anyLong());
    }

    @Test
    @DisplayName("동일 outbox에 두 번 claim 시도 시 첫 번째만 성공하고 두 번째는 건너뛴다")
    void claimCasAllowsOnlyFirstClaimToSucceed() {
        ProtectTargetIndexingOutboxDispatchUseCase useCase =
                new ProtectTargetIndexingOutboxDispatchUseCase(outboxRepository, publisher);
        ProtectTargetIndexingOutbox outbox = new ProtectTargetIndexingOutbox(
                1L,
                "brand",
                "브랜드 공식몰",
                ProtectTargetIndexingOutboxStatus.PENDING,
                null
        );

        // findPending이 같은 outbox를 두 번 반환하는 상황 (두 tick이 겹친 시뮬레이션)
        when(outboxRepository.findPending()).thenReturn(Flux.just(outbox, outbox));
        // 첫 번째 claim은 성공(1), 두 번째 claim은 경합 패배(0)
        when(outboxRepository.claim(1L))
                .thenReturn(Mono.just(1L))
                .thenReturn(Mono.just(0L));
        when(publisher.publish(any())).thenReturn(Mono.empty());
        when(outboxRepository.markPublished(1L)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.dispatchPending())
                .verifyComplete();

        verify(outboxRepository, times(2)).claim(1L);
        // publish는 첫 번째 claim 성공 시에만 호출
        verify(publisher, times(1)).publish(any());
        verify(outboxRepository, times(1)).markPublished(1L);
    }

    @Test
    @DisplayName("publish 실패 시 status가 PENDING으로 복구된다")
    void publishFailureTriggersPendingRollback() {
        ProtectTargetIndexingOutboxDispatchUseCase useCase =
                new ProtectTargetIndexingOutboxDispatchUseCase(outboxRepository, publisher);
        ProtectTargetIndexingOutbox outbox = new ProtectTargetIndexingOutbox(
                2L,
                "keyword",
                "키워드 설명",
                ProtectTargetIndexingOutboxStatus.PENDING,
                null
        );

        when(outboxRepository.findPending()).thenReturn(Flux.just(outbox));
        when(outboxRepository.claim(2L)).thenReturn(Mono.just(1L));
        when(publisher.publish(any())).thenReturn(Mono.error(new RuntimeException("kafka down")));
        when(outboxRepository.rollbackToPending(2L)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.dispatchPending())
                .verifyComplete();

        verify(outboxRepository).claim(2L);
        verify(publisher).publish(any());
        verify(outboxRepository, never()).markPublished(anyLong());
        verify(outboxRepository).rollbackToPending(2L);
    }
}
