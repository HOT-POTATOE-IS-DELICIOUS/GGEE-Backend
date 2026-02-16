package team.hotpotato.infrastructure.adapter.r2dbc.user;

import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import team.hotpotato.infrastructure.entity.UserEntity;
import team.hotpotato.application.port.output.UserReader;
import team.hotpotato.domain.user.User;

@Repository
@RequiredArgsConstructor
public class UserReaderAdapter implements UserReader {
    private final R2dbcEntityTemplate template;

    @Override
    public Mono<User> findByEmail(String email) {
        return template.selectOne(
                        Query.query(Criteria.where("email").is(email)),
                        UserEntity.class
                )
                .map(UserEntityMapper::toDomain);
    }
}
