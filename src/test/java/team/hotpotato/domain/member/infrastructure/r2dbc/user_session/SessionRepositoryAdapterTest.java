package team.hotpotato.domain.member.infrastructure.r2dbc.user_session;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import team.hotpotato.domain.member.domain.Session;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("세션 Repository 어댑터 단위 테스트")
class SessionRepositoryAdapterTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private R2dbcEntityTemplate template;

    @Test
    @DisplayName("findBySessionId는 sessionId로 조회한 세션을 도메인으로 반환한다")
    void findBySessionIdReturnsMappedSession() {
        SessionRepositoryAdapter adapter = new SessionRepositoryAdapter(template);
        UserSessionEntity entity = UserSessionEntity.builder()
                .id(1L)
                .userId(10L)
                .sessionId("sid-abc")
                .refreshTokenHash("abc123hash")
                .expiresAt(LocalDateTime.now().plusDays(14))
                .build();

        when(template.selectOne(any(), eq(UserSessionEntity.class))).thenReturn(Mono.just(entity));

        StepVerifier.create(adapter.findBySessionId("sid-abc"))
                .assertNext(session -> {
                    assertThat(session.sessionId()).isEqualTo("sid-abc");
                    assertThat(session.refreshTokenHash()).isEqualTo("abc123hash");
                    assertThat(session.userId()).isEqualTo(10L);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("findBySessionId는 세션이 없으면 empty를 반환한다")
    void findBySessionIdReturnsEmptyWhenNotFound() {
        SessionRepositoryAdapter adapter = new SessionRepositoryAdapter(template);
        when(template.selectOne(any(), eq(UserSessionEntity.class))).thenReturn(Mono.empty());

        StepVerifier.create(adapter.findBySessionId("missing-sid"))
                .verifyComplete();
    }

    @Test
    @DisplayName("updateRefreshTokenHash는 영향 row 수를 반환한다")
    void updateRefreshTokenHashReturnsAffectedCount() {
        SessionRepositoryAdapter adapter = new SessionRepositoryAdapter(template);
        when(template.update(any(), any(), eq(UserSessionEntity.class))).thenReturn(Mono.just(1L));

        StepVerifier.create(adapter.updateRefreshTokenHash("sid-abc", "old-hash", "new-hash", LocalDateTime.now().plusDays(14)))
                .assertNext(count -> assertThat(count).isEqualTo(1L))
                .verifyComplete();
    }

    @Test
    @DisplayName("updateRefreshTokenHash는 oldHash 불일치 시 0을 반환한다")
    void updateRefreshTokenHashReturnsZeroWhenHashMismatch() {
        SessionRepositoryAdapter adapter = new SessionRepositoryAdapter(template);
        when(template.update(any(), any(), eq(UserSessionEntity.class))).thenReturn(Mono.just(0L));

        StepVerifier.create(adapter.updateRefreshTokenHash("sid-abc", "wrong-old-hash", "new-hash", LocalDateTime.now().plusDays(14)))
                .assertNext(count -> assertThat(count).isEqualTo(0L))
                .verifyComplete();
    }

    @Test
    @DisplayName("invalidateBySessionId는 영향 row 수를 반환한다")
    void invalidateBySessionIdReturnsAffectedCount() {
        SessionRepositoryAdapter adapter = new SessionRepositoryAdapter(template);
        when(template.update(any(), any(), eq(UserSessionEntity.class))).thenReturn(Mono.just(1L));

        StepVerifier.create(adapter.invalidateBySessionId("sid-abc"))
                .assertNext(count -> assertThat(count).isEqualTo(1L))
                .verifyComplete();
    }

    @Test
    @DisplayName("save는 세션을 저장하고 도메인으로 반환한다")
    void saveReturnsMappedSession() {
        SessionRepositoryAdapter adapter = new SessionRepositoryAdapter(template);
        Session session = new Session(1L, 10L, "sid-abc", "hash-value", LocalDateTime.now().plusDays(14));
        UserSessionEntity savedEntity = UserSessionEntity.builder()
                .id(1L)
                .userId(10L)
                .sessionId("sid-abc")
                .refreshTokenHash("hash-value")
                .expiresAt(session.expiresAt())
                .build();

        when(template.insert(UserSessionEntity.class).using(any(UserSessionEntity.class))).thenReturn(Mono.just(savedEntity));

        StepVerifier.create(adapter.save(session))
                .assertNext(saved -> {
                    assertThat(saved.sessionId()).isEqualTo("sid-abc");
                    assertThat(saved.refreshTokenHash()).isEqualTo("hash-value");
                })
                .verifyComplete();
    }
}
