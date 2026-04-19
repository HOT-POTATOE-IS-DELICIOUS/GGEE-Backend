package team.hotpotato.domain.member.application.usecase.refresh;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.application.input.RefreshTokenResolver;
import team.hotpotato.domain.member.application.input.UserTokenRefresh;
import team.hotpotato.domain.member.application.output.SessionRepository;
import team.hotpotato.domain.member.application.output.TokenGenerator;
import team.hotpotato.domain.member.application.usecase.login.InvalidSessionException;
import team.hotpotato.domain.member.application.usecase.login.SessionExpiredException;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class UserTokenRefreshUseCase implements UserTokenRefresh {
    private final RefreshTokenResolver refreshTokenResolver;
    private final SessionRepository sessionRepository;
    private final TokenGenerator tokenGenerator;
    private final long refreshTokenActiveTimeSeconds;

    @Override
    public Mono<RefreshResult> refresh(RefreshCommand command) {
        return refreshTokenResolver.resolve(command.refreshToken())
                .flatMap(principal -> sessionRepository.findBySessionId(principal.sessionId())
                        .switchIfEmpty(Mono.error(InvalidSessionException.EXCEPTION))
                        .flatMap(session -> {
                            if (session.expiresAt().isBefore(LocalDateTime.now())) {
                                return Mono.error(SessionExpiredException.EXCEPTION);
                            }
                            if (!session.refreshToken().equals(command.refreshToken())) {
                                return Mono.error(InvalidSessionException.EXCEPTION);
                            }

                            String newAccessToken = tokenGenerator.generateAccessToken(principal);
                            String newRefreshToken = tokenGenerator.generateRefreshToken(principal);
                            LocalDateTime newExpiresAt = LocalDateTime.now().plusSeconds(refreshTokenActiveTimeSeconds);

                            return sessionRepository.updateRefreshToken(principal.sessionId(), newRefreshToken, newExpiresAt)
                                    .thenReturn(new RefreshResult(newAccessToken, newRefreshToken));
                        })
                );
    }
}
