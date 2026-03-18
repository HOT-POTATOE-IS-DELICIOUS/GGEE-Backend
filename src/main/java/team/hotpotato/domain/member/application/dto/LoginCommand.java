package team.hotpotato.domain.member.application.dto;

public record LoginCommand(
        String email,
        String password
) {
}
