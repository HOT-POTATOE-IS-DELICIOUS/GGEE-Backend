package team.hotpotato.domain.strategy.application.usecase.stream;

public record StreamStrategyChatCommand(
        Long userId,
        Long roomId,
        String message
) {
}
