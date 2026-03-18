package team.hotpotato.infrastructure.r2dbc.user;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import team.hotpotato.infrastructure.r2dbc.common.BaseEntity;

@Table("users")
@Getter
@Builder
public class UserEntity extends BaseEntity {
    @Column("role")
    String role;

    @Column("email")
    private String email;

    @Column("password")
    private String password;
}
