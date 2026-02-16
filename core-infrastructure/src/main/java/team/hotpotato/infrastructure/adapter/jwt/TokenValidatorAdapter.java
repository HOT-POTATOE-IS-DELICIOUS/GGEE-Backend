package team.hotpotato.infrastructure.adapter.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.hotpotato.infrastructure.adapter.jwt.enums.TokenType;
import team.hotpotato.infrastructure.adapter.jwt.exception.InvalidTokenException;
import team.hotpotato.infrastructure.adapter.jwt.exception.InvalidTokenTypeException;
import team.hotpotato.infrastructure.properties.TokenProperties;

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
