package team.hotpotato.domain.strategy.infrastructure.r2dbc.message;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import team.hotpotato.infrastructure.r2dbc.common.BaseEntity;

@Table("strategy_chat_messages")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StrategyChatMessageEntity extends BaseEntity {

    @Column("room_id")
    private Long roomId;

    @Column("role")
    private String role;

    @Column("content")
    private String content;

    @Column("intent")
    private String intent;

    @Column("refined_query")
    private String refinedQuery;

    @Column("meta_json")
    private String metaJson;

    @Column("ai_message_id")
    private String aiMessageId;
}
