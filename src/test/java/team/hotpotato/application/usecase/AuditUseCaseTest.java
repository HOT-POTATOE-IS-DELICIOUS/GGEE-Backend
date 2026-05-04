package team.hotpotato.application.usecase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import team.hotpotato.common.identity.IdGenerator;
import team.hotpotato.domain.audit.application.output.AuditRepository;
import team.hotpotato.domain.audit.application.output.AuditSource;
import team.hotpotato.domain.audit.application.usecase.audit.AuditCommand;
import team.hotpotato.domain.audit.application.usecase.audit.AuditUseCase;
import team.hotpotato.domain.audit.domain.Audit;
import team.hotpotato.domain.audit.domain.AuditAnalysis;
import team.hotpotato.domain.audit.domain.AuditReview;
import team.hotpotato.domain.audit.domain.AuditSentence;
import team.hotpotato.domain.audit.domain.AuditSuggestion;
import team.hotpotato.domain.protect.application.input.GetProtectByUserId;
import team.hotpotato.domain.protect.domain.Protect;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("입장문 검수 유스케이스 단위 테스트")
class AuditUseCaseTest {

    @Test
    @DisplayName("로그인한 사용자의 보호 대상 정보로 검수 후 결과를 저장하고 반환한다")
    void auditDelegatesToAuditSource() {
        String[] capturedArguments = new String[3];
        AuditSource auditSource = (protectTarget, protectTargetInfo, text) -> {
            capturedArguments[0] = protectTarget;
            capturedArguments[1] = protectTargetInfo;
            capturedArguments[2] = text;
            return Mono.just(new AuditAnalysis(
                    List.of(new AuditReview(
                            new AuditSentence("여자는 원래 그런 거 아니야?", 13, 27),
                            List.of("gender"),
                            List.of("성별 편향"),
                            List.of(new AuditSuggestion(
                                    0,
                                    15,
                                    "여자는 원래 그런 거 아니야?",
                                    "그것은 성별과 관계없이 개인차가 있습니다.",
                                    "이 문장에서 성별 고정관념 관련 편향 표현이 탐지되었습니다."
                            ))
                    ))
            ));
        };
        GetProtectByUserId getProtectByUserId = userId -> Mono.just(
                new Protect(42L, 7L, "백종원", "더본코리아")
        );
        IdGenerator idGenerator = () -> 9001L;
        Audit[] savedAudit = new Audit[1];
        AuditRepository auditRepository = audit -> {
            savedAudit[0] = audit;
            return Mono.just(audit);
        };
        AuditUseCase useCase = new AuditUseCase(auditSource, auditRepository, getProtectByUserId, idGenerator);

        StepVerifier.create(useCase.audit(new AuditCommand(7L, "여자는 원래 그런 거 아니야?")))
                .assertNext(result -> {
                    assertThat(capturedArguments).containsExactly(
                            "백종원",
                            "더본코리아",
                            "여자는 원래 그런 거 아니야?"
                    );
                    assertThat(result.auditId()).isEqualTo(9001L);
                    assertThat(result.reviews()).hasSize(1);
                    assertThat(result.reviews().getFirst().perspectiveIds()).containsExactly("gender");
                    assertThat(savedAudit[0]).isEqualTo(new Audit(
                            9001L,
                            7L,
                            "백종원",
                            "더본코리아",
                            "여자는 원래 그런 거 아니야?",
                            result.reviews()
                    ));
                })
                .verifyComplete();
    }
}
