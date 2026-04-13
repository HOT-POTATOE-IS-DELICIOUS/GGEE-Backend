package team.hotpotato.domain.member.application.usecase.logout;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.application.input.UserLogout;
import team.hotpotato.domain.member.application.output.SessionRepository;

@Service
@RequiredArgsConstructor
public class UserLogoutUseCase implements UserLogout {
    private final SessionRepository sessionRepository;

    @Override
    public Mono<Void> logout(LogoutCommand command) {
        return sessionRepository.invalidateByUserId(command.userId());
    }
}
