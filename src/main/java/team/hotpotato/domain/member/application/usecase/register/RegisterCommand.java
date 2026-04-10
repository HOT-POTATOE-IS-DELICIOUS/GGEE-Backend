package team.hotpotato.domain.member.application.usecase.register;

public record RegisterCommand(
        String email,
        String password,
        String protectTarget
) {
}
