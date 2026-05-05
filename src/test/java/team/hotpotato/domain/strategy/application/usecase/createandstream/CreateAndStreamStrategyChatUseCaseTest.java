package team.hotpotato.domain.strategy.application.usecase.createandstream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import team.hotpotato.common.exception.BusinessBaseException;
import team.hotpotato.common.exception.ErrorCode;
import team.hotpotato.common.identity.IdGenerator;
import team.hotpotato.domain.protect.application.input.GetProtectByUserId;
import team.hotpotato.domain.protect.domain.Protect;
import team.hotpotato.domain.strategy.application.output.StrategyAiClient;
import team.hotpotato.domain.strategy.application.output.StrategyChatMessageRepository;
import team.hotpotato.domain.strategy.application.output.StrategyChatRoomRepository;
import team.hotpotato.domain.strategy.domain.MessageRole;
import team.hotpotato.domain.strategy.domain.StrategyChatMessage;
import team.hotpotato.domain.strategy.domain.StrategyChatRoom;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateAndStreamStrategyChatUseCase 단위 테스트")
class CreateAndStreamStrategyChatUseCaseTest {

    @Mock
    private StrategyChatRoomRepository roomRepository;

    @Mock
    private StrategyChatMessageRepository messageRepository;

    @Mock
    private StrategyAiClient aiClient;

    @Mock
    private GetProtectByUserId getProtectByUserId;

    @Mock
    private IdGenerator idGenerator;

    private CreateAndStreamStrategyChatUseCase useCase;

    private static final Long USER_ID = 1L;
    private static final Long ROOM_ID = 10L;

    private final StrategyChatRoom savedRoom = new StrategyChatRoom(ROOM_ID, USER_ID, "test message", LocalDateTime.now(), LocalDateTime.now());
    private final Protect protect = new Protect(5L, USER_ID, "brand", "공식몰");

    @BeforeEach
    void setUp() {
        useCase = new CreateAndStreamStrategyChatUseCase(
                roomRepository, messageRepository, aiClient, getProtectByUserId, idGenerator, new ObjectMapper()
        );
    }

    private void stubRoomAndUserMessageSave() {
        when(idGenerator.generateId()).thenReturn(100L, 101L, 102L, 103L);
        when(roomRepository.save(any(StrategyChatRoom.class))).thenReturn(Mono.just(savedRoom));
        when(messageRepository.save(any(StrategyChatMessage.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(getProtectByUserId.get(USER_ID)).thenReturn(Mono.just(protect));
    }

    private ServerSentEvent<String> contentChunkEvent(String delta) {
        return ServerSentEvent.<String>builder()
                .event("content_chunk")
                .data("{\"delta\":\"" + delta + "\"}")
                .build();
    }

    private ServerSentEvent<String> doneEvent() {
        return ServerSentEvent.<String>builder()
                .event("done")
                .data("{\"message_id\":\"ai-msg-1\"}")
                .build();
    }

    @Test
    @DisplayName("done 이벤트 수신 시 assistant 메시지가 정상 저장된다")
    void saveAssistantMessageOnDoneEvent() {
        stubRoomAndUserMessageSave();
        when(aiClient.stream(any(), any(), any()))
                .thenReturn(Flux.just(contentChunkEvent("hello"), doneEvent()));

        StepVerifier.create(useCase.createAndStream(USER_ID, "test message"))
                .expectNextMatches(e -> "room_created".equals(e.event()))
                .expectNextMatches(e -> "content_chunk".equals(e.event()))
                .expectNextMatches(e -> "done".equals(e.event()))
                .verifyComplete();

        ArgumentCaptor<StrategyChatMessage> captor = ArgumentCaptor.forClass(StrategyChatMessage.class);
        verify(messageRepository, atLeastOnce()).save(captor.capture());

        StrategyChatMessage assistantMsg = captor.getAllValues().stream()
                .filter(m -> m.role() == MessageRole.ASSISTANT)
                .findFirst()
                .orElseThrow();
        assertThat(assistantMsg.content()).isEqualTo("hello");
        assertThat(assistantMsg.aiMessageId()).isEqualTo("ai-msg-1");
    }

    @Test
    @DisplayName("클라이언트 cancel 시 누적된 content가 있으면 partial 메시지가 저장된다")
    void savePartialMessageOnCancel() throws InterruptedException {
        stubRoomAndUserMessageSave();
        when(aiClient.stream(any(), any(), any()))
                .thenReturn(Flux.just(
                        contentChunkEvent("first"),
                        contentChunkEvent(" second"),
                        contentChunkEvent(" third")
                ).concatWith(Flux.never()));

        StepVerifier.create(useCase.createAndStream(USER_ID, "test message"))
                .expectNextMatches(e -> "room_created".equals(e.event()))
                .expectNextMatches(e -> "content_chunk".equals(e.event()))
                .thenCancel()
                .verify();

        // doFinally의 subscribe()는 fire-and-forget이므로 짧게 대기
        Thread.sleep(100);

        ArgumentCaptor<StrategyChatMessage> captor = ArgumentCaptor.forClass(StrategyChatMessage.class);
        verify(messageRepository, atLeastOnce()).save(captor.capture());

        boolean hasPartialAssistant = captor.getAllValues().stream()
                .anyMatch(m -> m.role() == MessageRole.ASSISTANT && m.aiMessageId() == null);
        assertThat(hasPartialAssistant).isTrue();
    }

    @Test
    @DisplayName("AI 오류 발생 시 BusinessBaseException이면 해당 ErrorCode를 포함한 error SSE frame이 emit된다")
    void emitErrorSseFrameOnBusinessException() {
        stubRoomAndUserMessageSave();
        BusinessBaseException cause = new BusinessBaseException(ErrorCode.STRATEGY_AI_SERVICE_UNAVAILABLE) {};
        when(aiClient.stream(any(), any(), any())).thenReturn(Flux.error(cause));

        StepVerifier.create(useCase.createAndStream(USER_ID, "test message"))
                .expectNextMatches(e -> "room_created".equals(e.event()))
                .expectNextMatches(e -> {
                    assertThat(e.event()).isEqualTo("error");
                    assertThat(e.data()).contains("STRATEGY_AI_SERVICE_UNAVAILABLE");
                    return true;
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("AI 오류 발생 시 일반 예외는 INTERNAL_SERVER_ERROR error SSE frame이 emit된다")
    void emitInternalServerErrorSseFrameOnGenericException() {
        stubRoomAndUserMessageSave();
        when(aiClient.stream(any(), any(), any())).thenReturn(Flux.error(new RuntimeException("network timeout")));

        StepVerifier.create(useCase.createAndStream(USER_ID, "test message"))
                .expectNextMatches(e -> "room_created".equals(e.event()))
                .expectNextMatches(e -> {
                    assertThat(e.event()).isEqualTo("error");
                    assertThat(e.data()).contains("INTERNAL_SERVER_ERROR");
                    return true;
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("AI 오류 발생 시 content가 없으면 partial 저장이 호출되지 않는다")
    void noPartialSaveWhenContentIsEmptyOnError() throws InterruptedException {
        stubRoomAndUserMessageSave();
        when(aiClient.stream(any(), any(), any())).thenReturn(Flux.error(new RuntimeException("immediate error")));

        StepVerifier.create(useCase.createAndStream(USER_ID, "test message"))
                .expectNextMatches(e -> "room_created".equals(e.event()))
                .expectNextMatches(e -> "error".equals(e.event()))
                .verifyComplete();

        Thread.sleep(100);

        ArgumentCaptor<StrategyChatMessage> captor = ArgumentCaptor.forClass(StrategyChatMessage.class);
        verify(messageRepository, atLeastOnce()).save(captor.capture());

        boolean hasAssistant = captor.getAllValues().stream()
                .anyMatch(m -> m.role() == MessageRole.ASSISTANT);
        assertThat(hasAssistant).isFalse();
    }
}
