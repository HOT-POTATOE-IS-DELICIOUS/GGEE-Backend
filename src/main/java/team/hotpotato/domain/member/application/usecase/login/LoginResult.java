package team.hotpotato.domain.member.application.usecase.login;

public record LoginResult(
        String accessToken,
        String refreshToken
) {
}
