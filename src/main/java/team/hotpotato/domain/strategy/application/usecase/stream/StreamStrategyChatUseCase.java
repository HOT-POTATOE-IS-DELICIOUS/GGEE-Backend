package team.hotpotato.domain.strategy.application.usecase.stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import team.hotpotato.common.exception.BusinessBaseException;
import team.hotpotato.common.exception.ErrorCode;
import team.hotpotato.common.identity.IdGenerator;
import team.hotpotato.domain.protect.application.input.GetProtectByUserId;
import team.hotpotato.domain.strategy.application.input.StreamStrategyChat;
import team.hotpotato.domain.strategy.application.output.StrategyAiClient;
import team.hotpotato.domain.strategy.application.output.StrategyChatMessageRepository;
import team.hotpotato.domain.strategy.application.output.StrategyChatRoomRepository;
import team.hotpotato.domain.strategy.domain.MessageRole;
import team.hotpotato.domain.strategy.domain.StrategyChatMessage;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamStrategyChatUseCase implements StreamStrategyChat {

    private final StrategyChatRoomRepository roomRepository;
    private final StrategyChatMessageRepository messageRepository;
    private final StrategyAiClient aiClient;
    private final GetProtectByUserId getProtectByUserId;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    @Override
    public Flux<ServerSentEvent<String>> stream(StreamStrategyChatCommand command) {
        return roomRepository.findByIdAndUserId(command.roomId(), command.userId())
                .switchIfEmpty(Mono.error(RoomNotFoundException.EXCEPTION))
                .flatMap(room -> messageRepository.save(new StrategyChatMessage(
                                idGenerator.generateId(),
                                room.id(),
                                MessageRole.USER,
                                command.message(),
                                null,
                                null,
                                null,
                                null,
                                null
                        ))
                        .then(roomRepository.updateLastChattedAt(room.id(), LocalDateTime.now()))
                        .thenReturn(room))
                .flatMapMany(room -> getProtectByUserId.get(command.userId())
                        .flatMapMany(protect -> streamWithPartialSave(
                                command.roomId(), command.message(), protect.target(), protect.info()
                        ))
                );
    }

    private Flux<ServerSentEvent<String>> streamWithPartialSave(Long roomId, String message, String target, String info) {
        return Flux.usingWhen(
                        Mono.fromSupplier(StreamState::new),
                        state -> aiClient.stream(message, target, info)
                                .doOnNext(event -> accumulate(event, state))
                                .concatMap(event -> handleDone(event, state, roomId)),
                        state -> savePartialIfNeeded(state, roomId),
                        (state, error) -> savePartialIfNeeded(state, roomId),
                        state -> savePartialIfNeeded(state, roomId)
                )
                .onErrorResume(e -> Flux.just(errorEvent(e)));
    }

    private Mono<ServerSentEvent<String>> handleDone(ServerSentEvent<String> event, StreamState state, Long roomId) {
        if (!"done".equals(event.event())) {
            return Mono.just(event);
        }
        return messageRepository.save(new StrategyChatMessage(
                        idGenerator.generateId(),
                        roomId,
                        MessageRole.ASSISTANT,
                        state.buffer.toString(),
                        state.intent.get(),
                        state.refinedQuery.get(),
                        state.metaJson.get(),
                        extractMessageId(event),
                        null
                ))
                .doOnSuccess(ignored -> state.saved.set(true))
                .doOnError(e -> log.warn("AI 메시지 저장 실패. roomId={}", roomId, e))
                .onErrorComplete()
                .thenReturn(event);
    }

    private Mono<Void> savePartialIfNeeded(StreamState state, Long roomId) {
        return Mono.defer(() -> {
            if (state.saved.get() || state.buffer.length() == 0) {
                return Mono.empty();
            }
            return messageRepository.save(new StrategyChatMessage(
                            idGenerator.generateId(),
                            roomId,
                            MessageRole.ASSISTANT,
                            state.buffer.toString(),
                            state.intent.get(),
                            state.refinedQuery.get(),
                            state.metaJson.get(),
                            null,
                            null
                    ))
                    .timeout(Duration.ofSeconds(5))
                    .doOnError(e -> log.warn("partial 메시지 저장 실패. roomId={}", roomId, e))
                    .onErrorComplete()
                    .then();
        });
    }

    private ServerSentEvent<String> errorEvent(Throwable e) {
        String code;
        if (e instanceof BusinessBaseException bex) {
            code = bex.getErrorCode().name();
        } else {
            code = ErrorCode.INTERNAL_SERVER_ERROR.name();
        }
        log.warn("Strategy AI 스트림 오류: {}", e.getMessage(), e);
        return ServerSentEvent.<String>builder()
                .event("error")
                .data("{\"code\":\"" + code + "\"}")
                .build();
    }

    private void accumulate(ServerSentEvent<String> event, StreamState state) {
        String eventType = event.event();
        String data = event.data();
        if (data == null) return;

        if ("content_chunk".equals(eventType)) {
            try {
                JsonNode node = objectMapper.readTree(data);
                JsonNode delta = node.get("delta");
                if (delta != null) {
                    state.buffer.append(delta.asText());
                }
            } catch (Exception e) {
                log.warn("content_chunk 파싱 실패: {}", data, e);
            }
        } else if ("intent_classified".equals(eventType)) {
            try {
                JsonNode node = objectMapper.readTree(data);
                JsonNode intent = node.get("intent");
                if (intent != null) {
                    state.intent.set(intent.asText());
                }
                JsonNode refinedQuery = node.get("refined_query");
                if (refinedQuery != null) {
                    state.refinedQuery.set(refinedQuery.asText());
                }
            } catch (Exception e) {
                log.warn("intent_classified 파싱 실패: {}", data, e);
            }
        } else if ("meta".equals(eventType)) {
            state.metaJson.set(data);
        }
    }

    private String extractMessageId(ServerSentEvent<String> event) {
        String data = event.data();
        if (data == null) return null;
        try {
            JsonNode node = objectMapper.readTree(data);
            JsonNode messageId = node.get("message_id");
            return messageId != null ? messageId.asText() : null;
        } catch (Exception e) {
            log.warn("message_id 파싱 실패: {}", data, e);
            return null;
        }
    }

    private static final class StreamState {
        final StringBuilder buffer = new StringBuilder();
        final AtomicReference<String> intent = new AtomicReference<>();
        final AtomicReference<String> refinedQuery = new AtomicReference<>();
        final AtomicReference<String> metaJson = new AtomicReference<>();
        final AtomicBoolean saved = new AtomicBoolean(false);
    }
}
