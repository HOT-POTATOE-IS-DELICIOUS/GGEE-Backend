package team.hotpotato.domain.strategy.infrastructure.r2dbc.room;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import team.hotpotato.infrastructure.r2dbc.common.BaseEntity;

@Table("strategy_chat_rooms")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StrategyChatRoomEntity extends BaseEntity {

    @Column("user_id")
    private Long userId;

    @Column("title")
    private String title;

    @Column("last_chatted_at")
    private java.time.LocalDateTime lastChattedAt;

}
