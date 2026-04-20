package team.hotpotato.domain.strategy.infrastructure.r2dbc.room;

import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.strategy.application.output.StrategyChatRoomRepository;
import team.hotpotato.domain.strategy.domain.StrategyChatRoom;

@Repository
@RequiredArgsConstructor
public class StrategyChatRoomRepositoryAdapter implements StrategyChatRoomRepository {

    private final R2dbcEntityTemplate template;

    @Override
    public Mono<StrategyChatRoom> save(StrategyChatRoom room) {
        return template.insert(StrategyChatRoomEntity.class)
                .using(StrategyChatRoomEntityMapper.toEntity(room))
                .map(StrategyChatRoomEntityMapper::toDomain);
    }

    @Override
    public Mono<StrategyChatRoom> findByIdAndUserId(Long id, Long userId) {
        return template.selectOne(
                Query.query(Criteria.where("id").is(id)
                        .and("user_id").is(userId)
                        .and("deleted").is(false)),
                StrategyChatRoomEntity.class
        ).map(StrategyChatRoomEntityMapper::toDomain);
    }

    @Override
    public Flux<StrategyChatRoom> findAllByUserId(Long userId) {
        return template.select(
                Query.query(Criteria.where("user_id").is(userId)
                        .and("deleted").is(false)),
                StrategyChatRoomEntity.class
        ).map(StrategyChatRoomEntityMapper::toDomain);
    }

    @Override
    public Mono<Void> updateLastChattedAt(Long roomId, java.time.LocalDateTime at) {
        return template.update(StrategyChatRoomEntity.class)
                .matching(Query.query(Criteria.where("id").is(roomId)))
                .apply(Update.update("last_chatted_at", at))
                .then();
    }
}
