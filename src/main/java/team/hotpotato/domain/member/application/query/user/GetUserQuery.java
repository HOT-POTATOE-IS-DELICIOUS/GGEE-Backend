package team.hotpotato.domain.member.application.query.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.application.input.GetUser;
import team.hotpotato.domain.member.application.output.UserRepository;
import team.hotpotato.domain.member.domain.User;

@Service
@RequiredArgsConstructor
public class GetUserQuery implements GetUser {
    private final UserRepository userRepository;

    @Override
    public Mono<User> get(Long userId) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(UserNotFoundException.EXCEPTION));
    }
}
