package team.hotpotato.infrastructure.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenValidatorAdapter {
    private final TokenProperties tokenProperties;

    public void validateAuthorizationHeader(String headerValue) {
        if (headerValue == null || !headerValue.startsWith(tokenProperties.prefix())) {
            throw InvalidTokenException.EXCEPTION;
        }
    }

    public void validateTokenType(TokenType targetTokenType, TokenType expectedTokenType) {
        if (targetTokenType != expectedTokenType) {
            throw InvalidTokenTypeException.EXCEPTION;
        }
    }
}
