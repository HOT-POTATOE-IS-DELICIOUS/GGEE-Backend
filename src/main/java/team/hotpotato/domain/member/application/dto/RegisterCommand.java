package team.hotpotato.domain.member.application.dto;

public record RegisterCommand(
        String email,
        String password
) {
}
