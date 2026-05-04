package team.hotpotato.domain.strategy.application.usecase.createandstream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import team.hotpotato.common.identity.IdGenerator;
import team.hotpotato.domain.protect.application.input.GetProtectByUserId;
import team.hotpotato.domain.strategy.application.input.CreateAndStreamStrategyChat;
import team.hotpotato.domain.strategy.application.output.StrategyAiClient;
import team.hotpotato.domain.strategy.application.output.StrategyChatMessageRepository;
import team.hotpotato.domain.strategy.application.output.StrategyChatRoomRepository;
import team.hotpotato.domain.strategy.domain.MessageRole;
import team.hotpotato.domain.strategy.domain.StrategyChatMessage;
import team.hotpotato.domain.strategy.domain.StrategyChatRoom;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateAndStreamStrategyChatUseCase implements CreateAndStreamStrategyChat {

    private final StrategyChatRoomRepository roomRepository;
    private final StrategyChatMessageRepository messageRepository;
    private final StrategyAiClient aiClient;
    private final GetProtectByUserId getProtectByUserId;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    @Override
    public Flux<ServerSentEvent<String>> createAndStream(Long userId, String message) {
        String title = message.substring(0, Math.min(12, message.length()));
        LocalDateTime now = LocalDateTime.now();

        return roomRepository.save(new StrategyChatRoom(
                        idGenerator.generateId(), userId, title, now, now
                ))
                .flatMap(room -> messageRepository.save(new StrategyChatMessage(
                                idGenerator.generateId(),
                                room.id(),
                                MessageRole.USER,
                                message,
                                null, null, null, null, null
                        ))
                        .thenReturn(room))
                .flatMapMany(room -> {
                    ServerSentEvent<String> roomCreatedEvent = ServerSentEvent.<String>builder()
                            .event("room_created")
                            .data("{\"roomId\":" + room.id() + "}")
                            .build();

                    StringBuilder contentBuffer = new StringBuilder();
                    AtomicReference<String> intentRef = new AtomicReference<>();
                    AtomicReference<String> refinedQueryRef = new AtomicReference<>();
                    AtomicReference<String> metaJsonRef = new AtomicReference<>();

                    return getProtectByUserId.get(userId)
                            .flatMapMany(protect ->
                                    aiClient.stream(message, protect.target(), protect.info())
                                            .doOnNext(event -> accumulate(event, contentBuffer, intentRef, refinedQueryRef, metaJsonRef))
                                            .concatMap(event -> {
                                                if ("done".equals(event.event())) {
                                                    return messageRepository.save(new StrategyChatMessage(
                                                                    idGenerator.generateId(),
                                                                    room.id(),
                                                                    MessageRole.ASSISTANT,
                                                                    contentBuffer.toString(),
                                                                    intentRef.get(),
                                                                    refinedQueryRef.get(),
                                                                    metaJsonRef.get(),
                                                                    extractMessageId(event),
                                                                    null
                                                            ))
                                                            .doOnError(e -> log.warn("AI 메시지 저장 실패. roomId={}", room.id(), e))
                                                            .onErrorComplete()
                                                            .thenReturn(event);
                                                }
                                                return Mono.just(event);
                                            })
                            )
                            .startWith(roomCreatedEvent);
                });
    }

    private void accumulate(ServerSentEvent<String> event, StringBuilder buffer,
                            AtomicReference<String> intentRef, AtomicReference<String> refinedQueryRef,
                            AtomicReference<String> metaJsonRef) {
        String eventType = event.event();
        String data = event.data();
        if (data == null) return;

        if ("content_chunk".equals(eventType)) {
            try {
                JsonNode node = objectMapper.readTree(data);
                JsonNode delta = node.get("delta");
                if (delta != null) {
                    buffer.append(delta.asText());
                }
            } catch (Exception e) {
                log.warn("content_chunk 파싱 실패: {}", data, e);
            }
        } else if ("intent_classified".equals(eventType)) {
            try {
                JsonNode node = objectMapper.readTree(data);
                JsonNode intent = node.get("intent");
                if (intent != null) {
                    intentRef.set(intent.asText());
                }
                JsonNode refinedQuery = node.get("refined_query");
                if (refinedQuery != null) {
                    refinedQueryRef.set(refinedQuery.asText());
                }
            } catch (Exception e) {
                log.warn("intent_classified 파싱 실패: {}", data, e);
            }
        } else if ("meta".equals(eventType)) {
            metaJsonRef.set(data);
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
}
