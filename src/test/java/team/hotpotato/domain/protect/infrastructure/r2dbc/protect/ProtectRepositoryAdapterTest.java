package team.hotpotato.domain.protect.infrastructure.r2dbc.protect;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import team.hotpotato.domain.protect.domain.Protect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Protect Repository 어댑터 단위 테스트")
class ProtectRepositoryAdapterTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private R2dbcEntityTemplate template;

    @Test
    @DisplayName("저장 성공 시 엔티티를 도메인으로 매핑해 반환한다")
    void saveReturnsMappedDomainProtect() {
        ProtectRepositoryAdapter adapter = new ProtectRepositoryAdapter(template);
        Protect protect = new Protect(11L, 7L, "brand", "공식몰");
        ProtectEntity savedEntity = ProtectEntity.builder()
                .id(11L)
                .userId(7L)
                .target("brand")
                .info("공식몰")
                .build();

        when(template.insert(ProtectEntity.class).using(any(ProtectEntity.class)))
                .thenReturn(Mono.just(savedEntity));

        StepVerifier.create(adapter.save(protect))
                .assertNext(saved -> {
                    assertThat(saved.id()).isEqualTo(11L);
                    assertThat(saved.userId()).isEqualTo(7L);
                    assertThat(saved.target()).isEqualTo("brand");
                    assertThat(saved.info()).isEqualTo("공식몰");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("findByUserId는 활성 행을 도메인으로 반환한다")
    void findByUserIdReturnsActiveProtect() {
        ProtectRepositoryAdapter adapter = new ProtectRepositoryAdapter(template);
        ProtectEntity entity = ProtectEntity.builder()
                .id(11L)
                .userId(7L)
                .target("brand")
                .info("공식몰")
                .build();

        when(template.selectOne(any(), eq(ProtectEntity.class)))
                .thenReturn(Mono.just(entity));

        StepVerifier.create(adapter.findByUserId(7L))
                .assertNext(found -> {
                    assertThat(found.id()).isEqualTo(11L);
                    assertThat(found.userId()).isEqualTo(7L);
                    assertThat(found.target()).isEqualTo("brand");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("findByUserId는 활성 행이 없으면 빈 Mono를 반환한다 (조회 책임만 가짐)")
    void findByUserIdReturnsEmptyWhenAbsent() {
        ProtectRepositoryAdapter adapter = new ProtectRepositoryAdapter(template);

        when(template.selectOne(any(), eq(ProtectEntity.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(adapter.findByUserId(999L))
                .verifyComplete();
    }
}
