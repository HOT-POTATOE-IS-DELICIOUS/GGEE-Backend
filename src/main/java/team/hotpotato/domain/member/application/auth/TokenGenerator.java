package team.hotpotato.domain.member.application.auth;

public interface TokenGenerator {
    String generateAccessToken(AuthPrincipal authPrincipal);

    String generateRefreshToken(AuthPrincipal authPrincipal);
}
