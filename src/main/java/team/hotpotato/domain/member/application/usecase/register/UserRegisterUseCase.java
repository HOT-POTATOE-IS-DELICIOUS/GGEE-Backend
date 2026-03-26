package team.hotpotato.domain.member.application.usecase.register;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import team.hotpotato.common.identity.IdGenerator;
import team.hotpotato.domain.member.application.output.UserAppender;
import team.hotpotato.domain.member.application.input.UserRegister;
import team.hotpotato.domain.member.domain.Role;
import team.hotpotato.domain.member.domain.User;

@Service
@RequiredArgsConstructor
public class UserRegisterUseCase implements UserRegister {
    private final UserAppender userAppender;
    private final IdGenerator idGenerator;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Mono<Void> register(RegisterCommand registerCommand) {
        return Mono.fromCallable(() -> new User(
                        idGenerator.generateId(),
                        registerCommand.email(),
                        passwordEncoder.encode(registerCommand.password()),
                        Role.USER
                ))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(userAppender::save)
				.onErrorMap(e -> {
					if (isDuplicateEmailError(e)) {
						return EmailAlreadyExistsException.EXCEPTION;
					}
					return e;
				})
                .then();
    }

	private boolean isDuplicateEmailError(Throwable e) {
		if (e instanceof DataIntegrityViolationException) {
			return e.getMessage().contains("email");
		}
		return false;
	}
}
