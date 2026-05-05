package team.hotpotato.domain.protect.application.usecase.indexing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import team.hotpotato.common.identity.IdGenerator;
import team.hotpotato.domain.protect.application.output.ProtectRepository;
import team.hotpotato.domain.protect.application.output.ProtectTargetIndexingOutboxRepository;
import team.hotpotato.domain.protect.domain.Protect;
import team.hotpotato.domain.protect.domain.ProtectTargetIndexingOutbox;
import team.hotpotato.domain.protect.domain.ProtectTargetIndexingOutboxStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Protect 인덱싱 유스케이스 단위 테스트")
class IndexProtectUseCaseTest {

    @Mock
    private ProtectRepository protectRepository;

    @Mock
    private ProtectTargetIndexingOutboxRepository outboxRepository;

    @Mock
    private IdGenerator idGenerator;

    private IndexProtectUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new IndexProtectUseCase(protectRepository, outboxRepository, idGenerator);
    }

    @Test
    @DisplayName("Protect 저장 성공 시 outbox에 PENDING 상태로 적재되고 두 ID가 반환된다")
    void indexSavesProtectAndOutbox() {
        when(idGenerator.generateId()).thenReturn(11L, 22L);
        when(protectRepository.save(any(Protect.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(outboxRepository.save(any(ProtectTargetIndexingOutbox.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.index(new IndexProtectCommand(7L, "brand", "공식몰")))
                .expectNext(new IndexProtectResult(11L, 22L))
                .verifyComplete();

        ArgumentCaptor<Protect> protectCaptor = ArgumentCaptor.forClass(Protect.class);
        ArgumentCaptor<ProtectTargetIndexingOutbox> outboxCaptor =
                ArgumentCaptor.forClass(ProtectTargetIndexingOutbox.class);
        verify(protectRepository).save(protectCaptor.capture());
        verify(outboxRepository).save(outboxCaptor.capture());

        Protect savedProtect = protectCaptor.getValue();
        assertThat(savedProtect.id()).isEqualTo(11L);
        assertThat(savedProtect.userId()).isEqualTo(7L);
        assertThat(savedProtect.target()).isEqualTo("brand");
        assertThat(savedProtect.info()).isEqualTo("공식몰");

        ProtectTargetIndexingOutbox savedOutbox = outboxCaptor.getValue();
        assertThat(savedOutbox.id()).isEqualTo(22L);
        assertThat(savedOutbox.protectTarget()).isEqualTo("brand");
        assertThat(savedOutbox.protectTargetInfo()).isEqualTo("공식몰");
        assertThat(savedOutbox.status()).isEqualTo(ProtectTargetIndexingOutboxStatus.PENDING);
        assertThat(savedOutbox.publishedAt()).isNull();
    }

    @Test
    @DisplayName("Protect 저장이 실패하면 outbox는 적재되지 않고 에러가 전파된다")
    void indexAbortsWhenProtectSaveFails() {
        RuntimeException expected = new RuntimeException("protect save failed");
        when(idGenerator.generateId()).thenReturn(11L);
        when(protectRepository.save(any(Protect.class))).thenReturn(Mono.error(expected));

        StepVerifier.create(useCase.index(new IndexProtectCommand(7L, "brand", "공식몰")))
                .expectErrorMatches(error -> error == expected)
                .verify();

        verify(protectRepository).save(any(Protect.class));
        verifyNoInteractions(outboxRepository);
    }

    @Test
    @DisplayName("Outbox 저장이 실패하면 에러가 전파된다 (롤백은 호출자 트랜잭션 책임)")
    void indexPropagatesOutboxFailure() {
        RuntimeException expected = new RuntimeException("outbox save failed");
        when(idGenerator.generateId()).thenReturn(11L, 22L);
        when(protectRepository.save(any(Protect.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(outboxRepository.save(any(ProtectTargetIndexingOutbox.class)))
                .thenReturn(Mono.error(expected));

        StepVerifier.create(useCase.index(new IndexProtectCommand(7L, "brand", "공식몰")))
                .expectErrorMatches(error -> error == expected)
                .verify();

        verify(protectRepository).save(any(Protect.class));
        verify(outboxRepository).save(any(ProtectTargetIndexingOutbox.class));
        verify(idGenerator, times(2)).generateId();
    }
}
