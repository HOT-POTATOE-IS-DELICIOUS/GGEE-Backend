package team.hotpotato.application.dto.result;

public record LoginResult(
        String accessToken,
        String refreshToken
) {
}
