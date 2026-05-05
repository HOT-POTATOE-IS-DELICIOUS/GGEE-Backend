package team.hotpotato.domain.protect.application.usecase.indexing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import team.hotpotato.common.identity.IdGenerator;
import team.hotpotato.domain.protect.application.input.IndexProtect;
import team.hotpotato.domain.protect.application.output.ProtectRepository;
import team.hotpotato.domain.protect.application.output.ProtectTargetIndexingOutboxRepository;
import team.hotpotato.domain.protect.domain.Protect;
import team.hotpotato.domain.protect.domain.ProtectTargetIndexingOutbox;
import team.hotpotato.domain.protect.domain.ProtectTargetIndexingOutboxStatus;

@Service
@RequiredArgsConstructor
public class IndexProtectUseCase implements IndexProtect {
    private final ProtectRepository protectRepository;
    private final ProtectTargetIndexingOutboxRepository outboxRepository;
    private final IdGenerator idGenerator;

    @Override
    public Mono<IndexProtectResult> index(IndexProtectCommand command) {
        Protect protect = new Protect(
                idGenerator.generateId(),
                command.userId(),
                command.target(),
                command.info()
        );

        return protectRepository.save(protect)
                .flatMap(savedProtect -> outboxRepository.save(new ProtectTargetIndexingOutbox(
                                idGenerator.generateId(),
                                savedProtect.target(),
                                savedProtect.info(),
                                ProtectTargetIndexingOutboxStatus.PENDING,
                                null
                        ))
                        .map(savedOutbox -> new IndexProtectResult(savedProtect.id(), savedOutbox.id()))
                );
    }
}
