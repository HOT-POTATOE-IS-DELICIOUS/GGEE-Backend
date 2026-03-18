package team.hotpotato.domain.member.api;

public record LoginResponse(
        String accessToken,
        String refreshToken
) {
}
