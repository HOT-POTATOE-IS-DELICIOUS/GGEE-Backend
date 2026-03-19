package team.hotpotato.infrastructure.r2dbc.user;

import lombok.NoArgsConstructor;
import team.hotpotato.domain.member.domain.Role;
import team.hotpotato.domain.member.domain.User;

@NoArgsConstructor
public final class UserEntityMapper {

    public static UserEntity toEntity(User user) {
        return UserEntity.builder()
                .id(user.id())
                .email(user.email())
                .password(user.password())
                .role(user.role().toString())
                .build();
    }

    public static User toDomain(UserEntity userEntity) {
        return new User(
                userEntity.getId(),
                userEntity.getEmail(),
                userEntity.getPassword(),
                Role.valueOf(userEntity.getRole())
        );
    }
}
