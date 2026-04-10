package team.hotpotato.application.usecase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import team.hotpotato.domain.member.application.output.ProtectTargetIndexingOutboxReader;
import team.hotpotato.domain.member.application.output.ProtectTargetIndexingOutboxUpdater;
import team.hotpotato.domain.member.application.output.ProtectTargetIndexingPublisher;
import team.hotpotato.domain.member.application.usecase.indexing.ProtectTargetIndexingOutboxDispatchUseCase;
import team.hotpotato.domain.member.domain.ProtectTargetIndexingOutbox;
import team.hotpotato.domain.member.domain.ProtectTargetIndexingOutboxStatus;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("보호 대상 인덱싱 outbox dispatcher 단위 테스트")
class ProtectTargetIndexingOutboxDispatchUseCaseTest {

    @Mock
    private ProtectTargetIndexingOutboxReader outboxReader;

    @Mock
    private ProtectTargetIndexingPublisher publisher;

    @Mock
    private ProtectTargetIndexingOutboxUpdater outboxUpdater;

    @Test
    @DisplayName("pending outbox를 발행하고 published 처리한다")
    void dispatchPublishesAndMarksPublished() {
        ProtectTargetIndexingOutboxDispatchUseCase useCase =
                new ProtectTargetIndexingOutboxDispatchUseCase(outboxReader, publisher, outboxUpdater);
        ProtectTargetIndexingOutbox outbox = new ProtectTargetIndexingOutbox(
                1L,
                "brand",
                ProtectTargetIndexingOutboxStatus.PENDING,
                null
        );

        when(outboxReader.findPending()).thenReturn(Flux.just(outbox));
        when(publisher.publish(any())).thenReturn(Mono.empty());
        when(outboxUpdater.markPublished(1L)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.dispatchPending())
                .verifyComplete();

        verify(outboxReader).findPending();
        verify(publisher).publish(any());
        verify(outboxUpdater).markPublished(1L);
    }

    @Test
    @DisplayName("발행 실패 outbox는 다음 주기에 재시도할 수 있도록 남겨둔다")
    void dispatchKeepsPendingWhenPublishFails() {
        ProtectTargetIndexingOutboxDispatchUseCase useCase =
                new ProtectTargetIndexingOutboxDispatchUseCase(outboxReader, publisher, outboxUpdater);
        ProtectTargetIndexingOutbox outbox = new ProtectTargetIndexingOutbox(
                1L,
                "brand",
                ProtectTargetIndexingOutboxStatus.PENDING,
                null
        );

        when(outboxReader.findPending()).thenReturn(Flux.just(outbox));
        when(publisher.publish(any())).thenReturn(Mono.error(new RuntimeException("publish failed")));

        StepVerifier.create(useCase.dispatchPending())
                .verifyComplete();

        verify(outboxReader).findPending();
        verify(publisher).publish(any());
        verifyNoInteractions(outboxUpdater);
    }

    @Test
    @DisplayName("이미 발행된 outbox가 없으면 종료한다")
    void dispatchCompletesWhenNoPendingOutboxExists() {
        ProtectTargetIndexingOutboxDispatchUseCase useCase =
                new ProtectTargetIndexingOutboxDispatchUseCase(outboxReader, publisher, outboxUpdater);

        when(outboxReader.findPending()).thenReturn(Flux.empty());

        StepVerifier.create(useCase.dispatchPending())
                .verifyComplete();

        verify(outboxReader).findPending();
        verifyNoInteractions(publisher, outboxUpdater);
    }
}
