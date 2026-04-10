package team.hotpotato.domain.member.application.usecase.register;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import team.hotpotato.domain.member.application.event.ProtectTargetIndexingMessage;
import team.hotpotato.common.identity.IdGenerator;
import team.hotpotato.domain.member.application.output.ProtectTargetIndexingPublisher;
import team.hotpotato.domain.member.application.output.UserAppender;
import team.hotpotato.domain.member.application.input.UserRegister;
import team.hotpotato.domain.member.domain.Role;
import team.hotpotato.domain.member.domain.User;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegisterUseCase implements UserRegister {
    private final UserAppender userAppender;
    private final ProtectTargetIndexingPublisher protectTargetIndexingPublisher;
    private final IdGenerator idGenerator;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Mono<Void> register(RegisterCommand registerCommand) {
        return Mono.fromCallable(() -> new User(
                        idGenerator.generateId(),
                        registerCommand.email(),
                        passwordEncoder.encode(registerCommand.password()),
                        Role.USER,
                        registerCommand.protectTarget()
                ))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(userAppender::save)
                .flatMap(savedUser -> protectTargetIndexingPublisher.publish(
                                new ProtectTargetIndexingMessage(savedUser.protectTarget())
                        )
                        .doOnError(error -> log.error(
                                "보호 대상 인덱싱 이벤트 발행에 실패했습니다. protectTarget={}",
                                savedUser.protectTarget(),
                                error
                        ))
                        .onErrorResume(error -> Mono.empty())
                )
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
