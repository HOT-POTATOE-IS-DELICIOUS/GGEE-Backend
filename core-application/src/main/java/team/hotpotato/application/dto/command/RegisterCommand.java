package team.hotpotato.application.dto.command;

public record RegisterCommand(
        String email,
        String password
) {
}
