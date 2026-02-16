package team.hotpotato.infrastructure.adapter.r2dbc.user;

import team.hotpotato.infrastructure.entity.UserEntity;
import team.hotpotato.domain.user.Role;
import team.hotpotato.domain.user.User;

public final class UserEntityMapper {
    private UserEntityMapper() {
    }

    public static UserEntity toEntity(User user) {
        return new UserEntity(
                user.userId(),
                user.email(),
                user.password(),
                user.role().name()
        );
    }

    public static User toDomain(UserEntity userEntity) {
        return new User(
                userEntity.userId(),
                userEntity.email(),
                userEntity.password(),
                Role.valueOf(userEntity.role())
        );
    }
}
