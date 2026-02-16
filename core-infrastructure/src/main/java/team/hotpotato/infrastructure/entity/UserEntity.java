package team.hotpotato.infrastructure.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("users")
public record UserEntity(
        @Id
        @Column("user_id")
        Long userId,
        @Column("email")
        String email,
        @Column("password")
        String password,
        @Column("role")
        String role
) {
}
