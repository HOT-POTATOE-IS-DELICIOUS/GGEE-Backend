package team.hotpotato.domain.member.application.usecase.login;

public record LoginCommand(
        String email,
        String password
) {
}
