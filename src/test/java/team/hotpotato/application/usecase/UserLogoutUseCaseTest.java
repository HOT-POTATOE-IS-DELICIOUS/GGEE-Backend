package team.hotpotato.application.usecase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import team.hotpotato.domain.member.application.output.SessionRepository;
import team.hotpotato.domain.member.application.usecase.logout.LogoutCommand;
import team.hotpotato.domain.member.application.usecase.logout.UserLogoutUseCase;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("로그아웃 유스케이스 단위 테스트")
class UserLogoutUseCaseTest {

    @Mock
    private SessionRepository sessionRepository;

    @Test
    @DisplayName("로그아웃은 사용자 세션을 무효화한다")
    void logoutInvalidatesUserSession() {
        UserLogoutUseCase useCase = new UserLogoutUseCase(sessionRepository);
        when(sessionRepository.invalidateByUserId(7L)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.logout(new LogoutCommand(7L)))
                .verifyComplete();

        verify(sessionRepository).invalidateByUserId(7L);
    }
}
