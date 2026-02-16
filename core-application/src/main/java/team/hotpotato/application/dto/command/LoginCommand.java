package team.hotpotato.application.dto.command;

public record LoginCommand(
        String email,
        String password
) {
}
