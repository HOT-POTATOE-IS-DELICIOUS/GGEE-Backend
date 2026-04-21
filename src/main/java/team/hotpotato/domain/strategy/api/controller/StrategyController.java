package team.hotpotato.domain.strategy.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import team.hotpotato.domain.strategy.api.dto.CreateRoomRequest;
import team.hotpotato.domain.strategy.api.dto.MessageResponse;
import team.hotpotato.domain.strategy.api.dto.RoomResponse;
import team.hotpotato.domain.strategy.api.dto.StreamRequest;
import team.hotpotato.domain.strategy.application.input.CreateAndStreamStrategyChat;
import team.hotpotato.domain.strategy.application.input.GetStrategyChatMessages;
import team.hotpotato.domain.strategy.application.input.GetStrategyChatRooms;
import team.hotpotato.domain.strategy.application.input.StreamStrategyChat;
import team.hotpotato.domain.strategy.application.usecase.stream.StreamStrategyChatCommand;
import team.hotpotato.security.CustomAuthPrincipal;

@RequiredArgsConstructor
@RequestMapping("/strategy/rooms")
@RestController
public class StrategyController {

    private final CreateAndStreamStrategyChat createAndStreamStrategyChat;
    private final StreamStrategyChat streamStrategyChat;
    private final GetStrategyChatRooms getStrategyChatRooms;
    private final GetStrategyChatMessages getStrategyChatMessages;

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> createRoom(
            @AuthenticationPrincipal CustomAuthPrincipal principal,
            @Valid @RequestBody CreateRoomRequest request
    ) {
        return createAndStreamStrategyChat.createAndStream(principal.userId(), request.message());
    }

    @GetMapping
    public Flux<RoomResponse> getRooms(
            @AuthenticationPrincipal CustomAuthPrincipal principal
    ) {
        return getStrategyChatRooms.getRooms(principal.userId())
                .map(result -> new RoomResponse(
                        result.roomId(),
                        result.title(),
                        result.lastChattedAt(),
                        result.createdAt()
                ));
    }

    @GetMapping("/{roomId}/messages")
    public Flux<MessageResponse> getMessages(
            @AuthenticationPrincipal CustomAuthPrincipal principal,
            @PathVariable Long roomId
    ) {
        return getStrategyChatMessages.getMessages(roomId, principal.userId())
                .map(result -> new MessageResponse(
                        result.messageId(),
                        result.role(),
                        result.content(),
                        result.intent(),
                        result.refinedQuery(),
                        result.metaJson(),
                        result.createdAt()
                ));
    }

    @PostMapping(value = "/{roomId}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(
            @AuthenticationPrincipal CustomAuthPrincipal principal,
            @PathVariable Long roomId,
            @Valid @RequestBody StreamRequest request
    ) {
        return streamStrategyChat.stream(new StreamStrategyChatCommand(
                principal.userId(),
                roomId,
                request.message()
        ));
    }
}
