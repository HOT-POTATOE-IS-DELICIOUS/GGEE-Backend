package team.hotpotato.domain.member.application.usecase.refresh;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.infrastructure.jwt.TokenProperties;
import team.hotpotato.domain.member.application.input.RefreshTokenResolver;
import team.hotpotato.domain.member.application.input.UserTokenRefresh;
import team.hotpotato.domain.member.application.output.RefreshTokenHasher;
import team.hotpotato.domain.member.application.output.SessionRepository;
import team.hotpotato.domain.member.application.output.TokenGenerator;
import team.hotpotato.domain.member.application.usecase.login.InvalidSessionException;
import team.hotpotato.domain.member.application.usecase.login.SessionExpiredException;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Service
public class UserTokenRefreshUseCase implements UserTokenRefresh {
    private final RefreshTokenResolver refreshTokenResolver;
    private final SessionRepository sessionRepository;
    private final TokenGenerator tokenGenerator;
    private final TokenProperties tokenProperties;
    private final RefreshTokenHasher refreshTokenHasher;
    private final Clock clock;
    private final Counter casMissCounter;

    public UserTokenRefreshUseCase(
            RefreshTokenResolver refreshTokenResolver,
            SessionRepository sessionRepository,
            TokenGenerator tokenGenerator,
            TokenProperties tokenProperties,
            RefreshTokenHasher refreshTokenHasher,
            Clock clock,
            MeterRegistry meterRegistry
    ) {
        this.refreshTokenResolver = refreshTokenResolver;
        this.sessionRepository = sessionRepository;
        this.tokenGenerator = tokenGenerator;
        this.tokenProperties = tokenProperties;
        this.refreshTokenHasher = refreshTokenHasher;
        this.clock = clock;
        // refresh token rotation 시 CAS UPDATE의 affected=0 빈도. race(다중 탭)와 reuse 공격이
        // 합쳐진 신호이므로 임계 알람으로만 사용한다. sessionId 같은 고-카디널리티 태그는 붙이지 않는다.
        this.casMissCounter = Counter.builder("auth.refresh.cas_miss")
                .description("Number of refresh CAS miss events (race or reuse, indistinguishable)")
                .register(meterRegistry);
    }

    @Override
    public Mono<RefreshResult> refresh(RefreshCommand command) {
        return refreshTokenResolver.resolve(command.refreshToken())
                .flatMap(principal -> {
                    String oldHash = refreshTokenHasher.hash(command.refreshToken());

                    String newAccessToken = tokenGenerator.generateAccessToken(principal);
                    String newRefreshToken = tokenGenerator.generateRefreshToken(principal);
                    String newHash = refreshTokenHasher.hash(newRefreshToken);
                    LocalDateTime newExpiresAt = LocalDateTime.now(clock).plusSeconds(tokenProperties.refreshTokenActiveTime());

                    return sessionRepository.findBySessionId(principal.sessionId())
                            .switchIfEmpty(Mono.error(InvalidSessionException.EXCEPTION))
                            .flatMap(session -> {
                                if (session.expiresAt().isBefore(LocalDateTime.now(clock))) {
                                    return Mono.error(SessionExpiredException.EXCEPTION);
                                }
                                return sessionRepository.updateRefreshTokenHash(principal.sessionId(), oldHash, newHash, newExpiresAt)
                                        .flatMap(affected -> {
                                            if (affected == 0) {
                                                casMissCounter.increment();
                                                log.warn("refresh CAS miss — race or reuse 가능. sessionId={}", principal.sessionId());
                                                return Mono.error(InvalidSessionException.EXCEPTION);
                                            }
                                            return Mono.just(new RefreshResult(newAccessToken, newRefreshToken));
                                        });
                            });
                });
    }
}
