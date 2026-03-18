package team.hotpotato.domain.member.application.dto;

public record LoginResult(
        String accessToken,
        String refreshToken
) {
}
