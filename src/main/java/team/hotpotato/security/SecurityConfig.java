package team.hotpotato.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import team.hotpotato.domain.member.application.input.TokenResolver;
import team.hotpotato.domain.member.application.output.SessionRepository;
import team.hotpotato.domain.member.infrastructure.jwt.TokenProperties;
import team.hotpotato.support.advice.ErrorCodeHttpStatusMapper;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final CorsConfigurationSource corsConfigurationSource;
    private final TokenResolver tokenResolver;
    private final SessionRepository sessionRepository;
    private final ErrorCodeHttpStatusMapper errorCodeHttpStatusMapper;
    private final TokenProperties tokenProperties;

    @Bean
    public AuthFilter authFilter() {
        return new AuthFilter(tokenResolver, sessionRepository, errorCodeHttpStatusMapper, tokenProperties);
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable);

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource));

        http
                .authorizeExchange(authorize -> {
                    authorize
                            .pathMatchers(SecurityPaths.PUBLIC_PATHS).permitAll()
                            .anyExchange().authenticated();
                });

        http
                .addFilterBefore(authFilter(), SecurityWebFiltersOrder.AUTHENTICATION);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
