package team.hotpotato.application.usecase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import team.hotpotato.domain.member.application.dto.ProtectTargetIndexingPublishCommand;
import team.hotpotato.domain.member.application.output.ProtectTargetIndexingOutboxRepository;
import team.hotpotato.domain.member.application.output.ProtectTargetIndexingPublisher;
import team.hotpotato.domain.member.application.usecase.indexing.ProtectTargetIndexingOutboxDispatchUseCase;
import team.hotpotato.domain.member.domain.ProtectTargetIndexingOutbox;
import team.hotpotato.domain.member.domain.ProtectTargetIndexingOutboxStatus;

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
        when(publisher.publish(any())).thenReturn(Mono.empty());
        when(outboxRepository.markPublished(1L)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.dispatchPending())
                .verifyComplete();

        ArgumentCaptor<ProtectTargetIndexingPublishCommand> messageCaptor =
                ArgumentCaptor.forClass(ProtectTargetIndexingPublishCommand.class);
        verify(outboxRepository).findPending();
        verify(publisher).publish(messageCaptor.capture());
        verify(outboxRepository).markPublished(1L);
        ProtectTargetIndexingPublishCommand publishedMessage = messageCaptor.getValue();
        verifyNoMoreInteractions(outboxRepository, publisher);
        org.junit.jupiter.api.Assertions.assertEquals(1L, publishedMessage.jobId());
        org.junit.jupiter.api.Assertions.assertEquals("brand", publishedMessage.keyword());
        org.junit.jupiter.api.Assertions.assertEquals("브랜드 공식몰", publishedMessage.protectTargetInfo());
    }

    @Test
    @DisplayName("발행 실패 outbox는 다음 주기에 재시도할 수 있도록 남겨둔다")
    void dispatchKeepsPendingWhenPublishFails() {
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
        when(publisher.publish(any())).thenReturn(Mono.error(new RuntimeException("publish failed")));

        StepVerifier.create(useCase.dispatchPending())
                .verifyComplete();

        ArgumentCaptor<ProtectTargetIndexingPublishCommand> messageCaptor =
                ArgumentCaptor.forClass(ProtectTargetIndexingPublishCommand.class);
        verify(outboxRepository).findPending();
        verify(publisher).publish(messageCaptor.capture());
        verify(outboxRepository, never()).markPublished(anyLong());
        ProtectTargetIndexingPublishCommand publishedMessage = messageCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(1L, publishedMessage.jobId());
        org.junit.jupiter.api.Assertions.assertEquals("brand", publishedMessage.keyword());
        org.junit.jupiter.api.Assertions.assertEquals("브랜드 공식몰", publishedMessage.protectTargetInfo());
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
}
