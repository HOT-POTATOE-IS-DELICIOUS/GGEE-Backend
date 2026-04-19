package team.hotpotato.domain.member.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.hotpotato.common.identity.IdGenerator;
import team.hotpotato.common.transaction.ReactiveTransactionRunner;
import team.hotpotato.domain.member.application.input.GetUser;
import team.hotpotato.domain.member.application.input.RefreshTokenResolver;
import team.hotpotato.domain.member.application.input.UserLogin;
import team.hotpotato.domain.member.application.input.UserLogout;
import team.hotpotato.domain.member.application.input.UserRegister;
import team.hotpotato.domain.member.application.input.UserTokenRefresh;
import team.hotpotato.domain.member.application.output.PasswordHasher;
import team.hotpotato.domain.member.application.output.ProtectTargetIndexingOutboxRepository;
import team.hotpotato.domain.member.application.output.ProtectTargetIndexingPublisher;
import team.hotpotato.domain.member.application.output.SessionRepository;
import team.hotpotato.domain.member.application.output.TokenGenerator;
import team.hotpotato.domain.member.application.output.UserRepository;
import team.hotpotato.domain.member.application.query.user.GetUserQuery;
import team.hotpotato.domain.member.application.usecase.indexing.ProtectTargetIndexingOutboxDispatchUseCase;
import team.hotpotato.domain.member.application.usecase.login.UserLoginUseCase;
import team.hotpotato.domain.member.application.usecase.logout.UserLogoutUseCase;
import team.hotpotato.domain.member.application.usecase.refresh.UserTokenRefreshUseCase;
import team.hotpotato.domain.member.application.usecase.register.UserRegisterUseCase;
import team.hotpotato.domain.member.infrastructure.jwt.TokenProperties;

@Configuration(proxyBeanMethods = false)
public class MemberUseCaseConfig {

    @Bean
    public GetUser getUser(UserRepository userRepository) {
        return new GetUserQuery(userRepository);
    }

    @Bean
    public UserRegister userRegister(
            UserRepository userRepository,
            ProtectTargetIndexingOutboxRepository outboxRepository,
            SessionRepository sessionRepository,
            TokenGenerator tokenGenerator,
            IdGenerator idGenerator,
            PasswordHasher passwordHasher,
            ReactiveTransactionRunner transactionRunner,
            TokenProperties tokenProperties
    ) {
        return new UserRegisterUseCase(
                userRepository,
                outboxRepository,
                sessionRepository,
                tokenGenerator,
                idGenerator,
                passwordHasher,
                transactionRunner,
                tokenProperties.refreshTokenActiveTime()
        );
    }

    @Bean
    public UserLogin userLogin(
            UserRepository userRepository,
            TokenGenerator tokenGenerator,
            PasswordHasher passwordHasher,
            SessionRepository sessionRepository,
            IdGenerator idGenerator,
            ReactiveTransactionRunner transactionRunner,
            TokenProperties tokenProperties
    ) {
        return new UserLoginUseCase(
                userRepository,
                tokenGenerator,
                passwordHasher,
                sessionRepository,
                idGenerator,
                transactionRunner,
                tokenProperties.refreshTokenActiveTime()
        );
    }

    @Bean
    public UserTokenRefresh userTokenRefresh(
            RefreshTokenResolver refreshTokenResolver,
            SessionRepository sessionRepository,
            TokenGenerator tokenGenerator,
            TokenProperties tokenProperties
    ) {
        return new UserTokenRefreshUseCase(
                refreshTokenResolver,
                sessionRepository,
                tokenGenerator,
                tokenProperties.refreshTokenActiveTime()
        );
    }

    @Bean
    public UserLogout userLogout(SessionRepository sessionRepository) {
        return new UserLogoutUseCase(sessionRepository);
    }

    @Bean
    public ProtectTargetIndexingOutboxDispatchUseCase protectTargetIndexingOutboxDispatchUseCase(
            ProtectTargetIndexingOutboxRepository outboxRepository,
            ProtectTargetIndexingPublisher publisher
    ) {
        return new ProtectTargetIndexingOutboxDispatchUseCase(outboxRepository, publisher);
    }
}
