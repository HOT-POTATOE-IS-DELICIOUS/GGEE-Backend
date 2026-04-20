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
import reactor.core.publisher.Mono;
import team.hotpotato.domain.strategy.api.dto.CreateRoomResponse;
import team.hotpotato.domain.strategy.api.dto.MessageResponse;
import team.hotpotato.domain.strategy.api.dto.RoomResponse;
import team.hotpotato.domain.strategy.api.dto.StreamRequest;
import team.hotpotato.domain.strategy.application.input.CreateStrategyChatRoom;
import team.hotpotato.domain.strategy.application.input.GetStrategyChatMessages;
import team.hotpotato.domain.strategy.application.input.GetStrategyChatRooms;
import team.hotpotato.domain.strategy.application.input.StreamStrategyChat;
import team.hotpotato.domain.strategy.application.usecase.create.CreateStrategyChatRoomCommand;
import team.hotpotato.domain.strategy.application.usecase.stream.StreamStrategyChatCommand;
import team.hotpotato.security.CustomAuthPrincipal;

@RequiredArgsConstructor
@RequestMapping("/strategy/rooms")
@RestController
public class StrategyController {

    private final CreateStrategyChatRoom createStrategyChatRoom;
    private final StreamStrategyChat streamStrategyChat;
    private final GetStrategyChatRooms getStrategyChatRooms;
    private final GetStrategyChatMessages getStrategyChatMessages;

    @PostMapping
    public Mono<CreateRoomResponse> createRoom(
            @AuthenticationPrincipal CustomAuthPrincipal principal
    ) {
        return createStrategyChatRoom.create(new CreateStrategyChatRoomCommand(principal.userId()))
                .map(result -> new CreateRoomResponse(
                        result.roomId(),
                        result.lastChattedAt(),
                        result.createdAt()
                ));
    }

    @GetMapping
    public Flux<RoomResponse> getRooms(
            @AuthenticationPrincipal CustomAuthPrincipal principal
    ) {
        return getStrategyChatRooms.getRooms(principal.userId())
                .map(result -> new RoomResponse(
                        result.roomId(),
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
