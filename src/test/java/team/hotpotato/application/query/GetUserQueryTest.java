package team.hotpotato.application.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import team.hotpotato.domain.member.application.output.UserRepository;
import team.hotpotato.domain.member.application.query.user.GetUserQuery;
import team.hotpotato.domain.member.application.query.user.UserNotFoundException;
import team.hotpotato.domain.member.domain.Role;
import team.hotpotato.domain.member.domain.User;

@DisplayName("유저 조회 쿼리 단위 테스트")
class GetUserQueryTest {

    @Test
    @DisplayName("유저가 존재하면 사용자 정보를 반환한다")
    void getReturnsUser() {
        UserRepository userRepository = new UserRepository() {
            @Override
            public Mono<User> findById(Long userId) {
                return Mono.just(new User(7L, "user@test.com", "encoded-password", Role.USER));
            }

            @Override
            public Mono<User> findByEmail(String email) {
                return Mono.empty();
            }

            @Override
            public Mono<User> save(User user) {
                return Mono.just(user);
            }
        };
        GetUserQuery getUserQuery = new GetUserQuery(userRepository);

        StepVerifier.create(getUserQuery.get(7L))
                .expectNext(new User(7L, "user@test.com", "encoded-password", Role.USER))
                .verifyComplete();
    }

    @Test
    @DisplayName("유저가 없으면 UserNotFoundException이 발생한다")
    void getThrowsUserNotFoundException() {
        UserRepository userRepository = new UserRepository() {
            @Override
            public Mono<User> findById(Long userId) {
                return Mono.empty();
            }

            @Override
            public Mono<User> findByEmail(String email) {
                return Mono.empty();
            }

            @Override
            public Mono<User> save(User user) {
                return Mono.just(user);
            }
        };
        GetUserQuery getUserQuery = new GetUserQuery(userRepository);

        StepVerifier.create(getUserQuery.get(7L))
                .expectError(UserNotFoundException.class)
                .verify();
    }
}
