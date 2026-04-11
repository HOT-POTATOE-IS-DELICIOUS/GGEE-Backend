package team.hotpotato.infrastructure.r2dbc.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import team.hotpotato.domain.member.application.usecase.register.EmailAlreadyExistsException;
import team.hotpotato.domain.member.domain.Role;
import team.hotpotato.domain.member.domain.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("사용자 Repository 어댑터 단위 테스트")
class UserRepositoryAdapterTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private R2dbcEntityTemplate template;

    @Test
    @DisplayName("저장 성공 시 엔티티를 도메인으로 매핑해 반환한다")
    void saveReturnsMappedDomainUser() {
        UserRepositoryAdapter adapter = new UserRepositoryAdapter(template);
        User user = new User(1L, "user@test.com", "encoded", Role.USER, "brand");
        UserEntity savedEntity = UserEntity.builder()
                .id(1L)
                .email("user@test.com")
                .password("encoded")
                .role("USER")
                .protectTarget("brand")
                .build();

        when(template.insert(UserEntity.class).using(any(UserEntity.class))).thenReturn(Mono.just(savedEntity));

        StepVerifier.create(adapter.save(user))
                .assertNext(savedUser -> {
                    assertThat(savedUser.id()).isEqualTo(1L);
                    assertThat(savedUser.email()).isEqualTo("user@test.com");
                    assertThat(savedUser.role()).isEqualTo(Role.USER);
                    assertThat(savedUser.protectTarget()).isEqualTo("brand");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("중복 이메일 제약조건 오류는 EmailAlreadyExistsException으로 변환한다")
    void saveMapsDuplicateEmailError() {
        UserRepositoryAdapter adapter = new UserRepositoryAdapter(template);
        User user = new User(1L, "user@test.com", "encoded", Role.USER, "brand");

        when(template.insert(UserEntity.class).using(any(UserEntity.class)))
                .thenReturn(Mono.error(new DataIntegrityViolationException("Duplicate entry")));

        StepVerifier.create(adapter.save(user))
                .expectError(EmailAlreadyExistsException.class)
                .verify();
    }
}
