package team.hotpotato.domain.member.api.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken
) {
}
