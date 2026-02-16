package team.hotpotato.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import team.hotpotato.application.dto.command.RegisterCommand;
import team.hotpotato.application.port.output.IdGenerator;
import team.hotpotato.application.port.output.UserAppender;
import team.hotpotato.domain.user.Role;
import team.hotpotato.domain.user.User;

@Service
@RequiredArgsConstructor
public class UserRegisterUseCase implements UserRegister {
    private final UserAppender userAppender;
    private final IdGenerator idGenerator;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Mono<Void> register(RegisterCommand registerCommand) {
        return userAppender.save(new User(
                        idGenerator.generateId(),
                        registerCommand.email(),
                        passwordEncoder.encode(registerCommand.password()),
                        Role.USER
                ))
                .then();
    }
}
