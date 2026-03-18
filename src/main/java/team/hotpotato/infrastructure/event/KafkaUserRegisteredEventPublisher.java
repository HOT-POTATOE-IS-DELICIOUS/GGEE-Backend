package team.hotpotato.infrastructure.event;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.domain.User;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class KafkaUserRegisteredEventPublisher {
    private final KafkaOperations<String, Object> kafkaTemplate;

    public Mono<Void> publish(User user) {
        UserRegisteredEvent event = new UserRegisteredEvent(
                user.userId(),
                user.email(),
                user.role().name(),
                Instant.now()
        );

        return Mono.fromFuture(
                        kafkaTemplate.send(
//                                userEventProperties.registeredTopic(),
                                user.userId().toString(),
                                event
                        )
                )
                .then();
    }
}
