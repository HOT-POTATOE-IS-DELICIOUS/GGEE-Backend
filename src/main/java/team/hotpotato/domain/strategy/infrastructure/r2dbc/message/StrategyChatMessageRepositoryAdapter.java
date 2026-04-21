package team.hotpotato.domain.strategy.infrastructure.r2dbc.message;

import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.strategy.application.output.StrategyChatMessageRepository;
import team.hotpotato.domain.strategy.domain.StrategyChatMessage;

@Repository
@RequiredArgsConstructor
public class StrategyChatMessageRepositoryAdapter implements StrategyChatMessageRepository {

    private final R2dbcEntityTemplate template;

    @Override
    public Mono<StrategyChatMessage> save(StrategyChatMessage message) {
        return template.insert(StrategyChatMessageEntity.class)
                .using(StrategyChatMessageEntityMapper.toEntity(message))
                .map(StrategyChatMessageEntityMapper::toDomain);
    }

    @Override
    public Flux<StrategyChatMessage> findAllByRoomId(Long roomId) {
        return template.select(
                Query.query(Criteria.where("room_id").is(roomId)
                        .and("deleted").is(false)),
                StrategyChatMessageEntity.class
        ).map(StrategyChatMessageEntityMapper::toDomain);
    }
}
