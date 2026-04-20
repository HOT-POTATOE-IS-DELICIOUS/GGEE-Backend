package team.hotpotato.domain.strategy.infrastructure.r2dbc.room;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import team.hotpotato.domain.strategy.domain.StrategyChatRoom;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StrategyChatRoomEntityMapper {

    public static StrategyChatRoomEntity toEntity(StrategyChatRoom room) {
        return StrategyChatRoomEntity.builder()
                .id(room.id())
                .userId(room.userId())
                .lastChattedAt(room.lastChattedAt())
                .build();
    }

    public static StrategyChatRoom toDomain(StrategyChatRoomEntity entity) {
        return new StrategyChatRoom(
                entity.getId(),
                entity.getUserId(),
                entity.getLastChattedAt(),
                entity.getCreatedAt()
        );
    }
}
