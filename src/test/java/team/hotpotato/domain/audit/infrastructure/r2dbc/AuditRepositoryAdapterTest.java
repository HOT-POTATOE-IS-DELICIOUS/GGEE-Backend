package team.hotpotato.domain.audit.infrastructure.r2dbc;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import team.hotpotato.domain.audit.domain.Audit;
import team.hotpotato.domain.audit.domain.AuditReview;
import team.hotpotato.domain.audit.domain.AuditSentence;
import team.hotpotato.domain.audit.domain.AuditSuggestion;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("검수 Repository 어댑터 단위 테스트")
class AuditRepositoryAdapterTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private R2dbcEntityTemplate template;

    @Test
    @DisplayName("저장 시 검수 결과를 JSON으로 직렬화해 insert 한다")
    void saveSerializesReviewsAndReturnsAudit() {
        AuditRepositoryAdapter adapter = new AuditRepositoryAdapter(template, new ObjectMapper());
        Audit audit = new Audit(
                1001L,
                7L,
                "백종원",
                "더본코리아",
                "홍어들이 또 난리났네.",
                "550e8400-e29b-41d4-a716-446655440000",
                List.of(new AuditReview(
                        new AuditSentence("홍어들이 또 난리났네.", 0, 12),
                        List.of("community"),
                        List.of("커뮤니티 성향"),
                        List.of(new AuditSuggestion(
                                0,
                                4,
                                "홍어들이",
                                "그 사람들이",
                                "이 문장에서 일베 커뮤니티 특유의 언어·문화 표현이 탐지되었습니다."
                        ))
                ))
        );

        when(template.insert(AuditEntity.class).using(any(AuditEntity.class)))
                .thenReturn(Mono.just(AuditEntity.builder().id(1001L).build()));

        StepVerifier.create(adapter.save(audit))
                .assertNext(savedAudit -> assertThat(savedAudit).isEqualTo(audit))
                .verifyComplete();

        ArgumentCaptor<AuditEntity> entityCaptor = ArgumentCaptor.forClass(AuditEntity.class);
        verify(template.insert(AuditEntity.class)).using(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getId()).isEqualTo(1001L);
        assertThat(entityCaptor.getValue().getUserId()).isEqualTo(7L);
        assertThat(entityCaptor.getValue().getProtectTarget()).isEqualTo("백종원");
        assertThat(entityCaptor.getValue().getMessageId()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        assertThat(entityCaptor.getValue().getReviewsJson()).contains("community");
    }
}
