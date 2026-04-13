package team.hotpotato.domain.member.application.usecase.register;

public record RegisterResult(
        String indexingJobId,
        String accessToken,
        String refreshToken
) {
}
