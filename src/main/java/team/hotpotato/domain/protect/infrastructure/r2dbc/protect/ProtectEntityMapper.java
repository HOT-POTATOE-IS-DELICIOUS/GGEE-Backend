package team.hotpotato.domain.protect.infrastructure.r2dbc.protect;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import team.hotpotato.domain.protect.domain.Protect;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProtectEntityMapper {

    public static ProtectEntity toEntity(Protect protect) {
        return ProtectEntity.builder()
                .id(protect.id())
                .userId(protect.userId())
                .target(protect.target())
                .info(protect.info())
                .build();
    }

    public static Protect toDomain(ProtectEntity entity) {
        return new Protect(
                entity.getId(),
                entity.getUserId(),
                entity.getTarget(),
                entity.getInfo()
        );
    }
}
