package team.hotpotato.application.port.output;

import team.hotpotato.application.dto.model.AuthPrincipal;

public interface TokenGenerator {
    String generateAccessToken(AuthPrincipal authPrincipal);

    String generateRefreshToken(AuthPrincipal authPrincipal);
}
