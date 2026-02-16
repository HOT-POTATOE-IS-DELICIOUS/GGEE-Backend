package team.hotpotato.api.dto.response;

public record LoginResponse(
        String accessToken,
        String refreshToken
) {
}
