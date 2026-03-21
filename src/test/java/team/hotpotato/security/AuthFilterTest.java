package team.hotpotato.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import team.hotpotato.domain.member.application.auth.AuthPrincipal;
import team.hotpotato.domain.member.application.auth.TokenResolver;
import team.hotpotato.domain.member.domain.Role;
import team.hotpotato.infrastructure.jwt.InvalidTokenException;
import team.hotpotato.infrastructure.jwt.TokenProperties;
import team.hotpotato.support.advice.ErrorCodeHttpStatusMapper;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("인증 필터 단위 테스트")
class AuthFilterTest {

    @Test
    @DisplayName("공개 경로는 잘못된 토큰 헤더가 있어도 요청을 통과시킨다")
    void invalidTokenOnPublicPathIsIgnored() {
        TokenResolver tokenResolver = mock(TokenResolver.class);
        when(tokenResolver.resolve("Bearer invalid")).thenReturn(Mono.error(InvalidTokenException.EXCEPTION));

        AuthFilter authFilter = new AuthFilter(
                tokenResolver,
                new ErrorCodeHttpStatusMapper(),
                new TokenProperties(3600L, 7200L, "Bearer ", "Authorization", "ignored")
        );

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/auth/login")
                        .header("Authorization", "Bearer invalid")
                        .build()
        );
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        WebFilterChain chain = webExchange -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        StepVerifier.create(authFilter.filter(exchange, chain))
                .verifyComplete();

        assertThat(chainCalled).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("보호 경로는 잘못된 토큰 헤더면 에러 응답을 반환한다")
    void invalidTokenOnProtectedPathReturnsErrorResponse() {
        TokenResolver tokenResolver = mock(TokenResolver.class);
        when(tokenResolver.resolve("Bearer invalid")).thenReturn(Mono.error(InvalidTokenException.EXCEPTION));

        AuthFilter authFilter = new AuthFilter(
                tokenResolver,
                new ErrorCodeHttpStatusMapper(),
                new TokenProperties(3600L, 7200L, "Bearer ", "Authorization", "ignored")
        );

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/members/me")
                        .header("Authorization", "Bearer invalid")
                        .build()
        );
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        WebFilterChain chain = webExchange -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        StepVerifier.create(authFilter.filter(exchange, chain))
                .verifyComplete();

        assertThat(chainCalled).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("설정된 헤더 이름으로만 토큰을 읽고 SecurityContext에는 ROLE_ 권한을 넣는다")
    void filterUsesConfiguredHeaderAndWritesSecurityContext() {
        TokenResolver tokenResolver = mock(TokenResolver.class);
        when(tokenResolver.resolve("token-value")).thenReturn(Mono.just(new AuthPrincipal(7L, Role.USER)));

        AuthFilter authFilter = new AuthFilter(
                tokenResolver,
                new ErrorCodeHttpStatusMapper(),
                new TokenProperties(3600L, 7200L, "Bearer ", "X-AUTH", "ignored")
        );

        MockServerWebExchange ignoredExchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/members/me")
                        .header("Authorization", "token-value")
                        .build()
        );

        StepVerifier.create(authFilter.filter(ignoredExchange, webExchange -> Mono.empty()))
                .verifyComplete();

        verifyNoInteractions(tokenResolver);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/members/me")
                        .header("X-AUTH", "token-value")
                        .build()
        );
        AtomicReference<Authentication> authentication = new AtomicReference<>();
        WebFilterChain chain = webExchange -> ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> securityContext.getAuthentication())
                .doOnNext(authentication::set)
                .then();

        StepVerifier.create(authFilter.filter(exchange, chain))
                .verifyComplete();

        assertThat(authentication.get()).isNotNull();
        assertThat(authentication.get().getPrincipal()).isEqualTo(7L);
        assertThat(authentication.get().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }
}
