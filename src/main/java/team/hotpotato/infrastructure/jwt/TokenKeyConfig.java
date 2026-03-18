package team.hotpotato.infrastructure.jwt;

import io.jsonwebtoken.security.Keys;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.util.Base64;

@Configuration
public class TokenKeyConfig {
    @Bean
    public SecretKey secretKey(TokenProperties tokenProperties) {
        return Keys.hmacShaKeyFor(
                Base64.getDecoder().decode(tokenProperties.secretKey())
        );
    }
}
