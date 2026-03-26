package team.hotpotato.domain.member.application.output;

import team.hotpotato.domain.member.application.model.AuthPrincipal;

public interface TokenGenerator {
    String generateAccessToken(AuthPrincipal authPrincipal);

    String generateRefreshToken(AuthPrincipal authPrincipal);
}
