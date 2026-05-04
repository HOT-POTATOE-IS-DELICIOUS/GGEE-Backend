package team.hotpotato.domain.protect.application.usecase.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.protect.application.input.GetProtectByUserId;
import team.hotpotato.domain.protect.application.output.ProtectRepository;
import team.hotpotato.domain.protect.domain.Protect;

@Service
@RequiredArgsConstructor
public class GetProtectByUserIdQuery implements GetProtectByUserId {
    private final ProtectRepository protectRepository;

    @Override
    public Mono<Protect> get(Long userId) {
        return protectRepository.findByUserId(userId)
                .switchIfEmpty(Mono.error(ProtectNotFoundException.EXCEPTION));
    }
}
