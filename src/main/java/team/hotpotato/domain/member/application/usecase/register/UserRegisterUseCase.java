package team.hotpotato.domain.member.application.usecase.register;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import team.hotpotato.common.identity.IdGenerator;
import team.hotpotato.domain.member.application.output.ProtectTargetIndexingOutboxAppender;
import team.hotpotato.domain.member.application.output.UserAppender;
import team.hotpotato.domain.member.application.input.UserRegister;
import team.hotpotato.domain.member.domain.ProtectTargetIndexingOutbox;
import team.hotpotato.domain.member.domain.ProtectTargetIndexingOutboxStatus;
import team.hotpotato.domain.member.domain.Role;
import team.hotpotato.domain.member.domain.User;

@Service
@RequiredArgsConstructor
public class UserRegisterUseCase implements UserRegister {
    private final UserAppender userAppender;
    private final ProtectTargetIndexingOutboxAppender outboxAppender;
    private final IdGenerator idGenerator;
    private final PasswordEncoder passwordEncoder;
    private final TransactionalOperator transactionalOperator;

    @Override
    public Mono<Void> register(RegisterCommand registerCommand) {
        return createUser(registerCommand)
                .flatMap(user -> userAppender.save(user)
                        .flatMap(this::saveOutbox)
                )
                .as(transactionalOperator::transactional)
                .onErrorMap(e -> {
                    if (isDuplicateEmailError(e)) {
                        return EmailAlreadyExistsException.EXCEPTION;
                    }
                    return e;
                })
                .then();
    }

    private Mono<User> createUser(RegisterCommand registerCommand) {
        return Mono.fromCallable(() -> new User(
                        idGenerator.generateId(),
                        registerCommand.email(),
                        passwordEncoder.encode(registerCommand.password()),
                        Role.USER,
                        registerCommand.protectTarget()
                ))
                .subscribeOn(Schedulers.boundedElastic())
                .cast(User.class);
    }

    private Mono<ProtectTargetIndexingOutbox> saveOutbox(User savedUser) {
        return outboxAppender.save(new ProtectTargetIndexingOutbox(
                idGenerator.generateId(),
                savedUser.protectTarget(),
                ProtectTargetIndexingOutboxStatus.PENDING,
                null
        ));
    }

    private boolean isDuplicateEmailError(Throwable e) {
        if (e instanceof DataIntegrityViolationException) {
            return e.getMessage().contains("email");
        }
        return false;
    }
}
