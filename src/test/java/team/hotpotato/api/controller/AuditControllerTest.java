package team.hotpotato.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import team.hotpotato.GgeeBackendApplication;
import team.hotpotato.domain.audit.application.input.AuditStatement;
import team.hotpotato.domain.audit.application.usecase.audit.AuditCommand;
import team.hotpotato.domain.audit.application.usecase.audit.AuditResult;
import team.hotpotato.domain.audit.application.usecase.audit.AuditServiceUnavailableException;
import team.hotpotato.domain.audit.domain.AuditReview;
import team.hotpotato.domain.audit.domain.AuditSentence;
import team.hotpotato.domain.audit.domain.AuditSuggestion;
import team.hotpotato.domain.member.application.input.TokenResolver;
import team.hotpotato.domain.member.application.model.AuthPrincipal;
import team.hotpotato.domain.member.application.output.SessionRepository;
import team.hotpotato.domain.member.domain.Role;
import team.hotpotato.domain.member.domain.Session;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = GgeeBackendApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("입장문 검수 API 통합 테스트")
class AuditControllerTest {

    private WebTestClient webTestClient;

    @LocalServerPort
    private int port;

    @MockitoBean
    private AuditStatement auditStatement;

    @MockitoBean
    private TokenResolver tokenResolver;

    @MockitoBean
    private SessionRepository sessionRepository;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    @DisplayName("입장문 검수 요청은 command로 변환되어 응답을 반환한다")
    void auditReturnsReviewResponse() {
        mockAuthenticatedUser();
        when(auditStatement.audit(any(AuditCommand.class)))
                .thenReturn(Mono.just(new AuditResult(
                        1001L,
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
                )));

        webTestClient.post()
                .uri("/audit")
                .header("Authorization", "Bearer valid-access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "text": "홍어들이 또 난리났네."
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.audit_id").isEqualTo(1001)
                .jsonPath("$.reviews[0].sentence.sentence_text").isEqualTo("홍어들이 또 난리났네.")
                .jsonPath("$.reviews[0].sentence.start_offset").isEqualTo(0)
                .jsonPath("$.reviews[0].perspective_ids[0]").isEqualTo("community")
                .jsonPath("$.reviews[0].perspective_labels[0]").isEqualTo("커뮤니티 성향")
                .jsonPath("$.reviews[0].suggestions[0].before").isEqualTo("홍어들이")
                .jsonPath("$.reviews[0].suggestions[0].after").isEqualTo("그 사람들이");

        ArgumentCaptor<AuditCommand> commandCaptor = ArgumentCaptor.forClass(AuditCommand.class);
        verify(auditStatement).audit(commandCaptor.capture());
        assertThat(commandCaptor.getValue()).isEqualTo(new AuditCommand(7L, "홍어들이 또 난리났네."));
    }

    @Test
    @DisplayName("검수 서비스 통신 실패는 503을 반환한다")
    void auditReturnsServiceUnavailableWhenAuditServiceFails() {
        mockAuthenticatedUser();
        when(auditStatement.audit(any(AuditCommand.class)))
                .thenReturn(Mono.error(AuditServiceUnavailableException.EXCEPTION));

        webTestClient.post()
                .uri("/audit")
                .header("Authorization", "Bearer valid-access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "text": "검수할 입장문"
                        }
                        """)
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody(String.class).isEqualTo("입장문 검수 서버와 통신할 수 없습니다.");
    }

    @Test
    @DisplayName("잘못된 요청 본문은 400을 반환한다")
    void auditReturnsBadRequestWhenRequestBodyIsInvalid() {
        mockAuthenticatedUser();

        webTestClient.post()
                .uri("/audit")
                .header("Authorization", "Bearer valid-access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "text": ""
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class).isEqualTo("검수할 입장문을 입력해주세요.");

        verifyNoInteractions(auditStatement);
    }

    @Test
    @DisplayName("인증 정보가 없으면 401을 반환한다")
    void auditReturnsUnauthorizedWhenAuthorizationHeaderIsMissing() {
        webTestClient.post()
                .uri("/audit")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "text": "검수할 입장문"
                        }
                        """)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private void mockAuthenticatedUser() {
        when(tokenResolver.resolve("Bearer valid-access-token"))
                .thenReturn(Mono.just(new AuthPrincipal(7L, Role.USER, "audit-session-id")));
        when(sessionRepository.findActiveByUserId(7L))
                .thenReturn(Mono.just(new Session(
                        1L,
                        7L,
                        "audit-session-id",
                        "refresh-token",
                        LocalDateTime.now().plusHours(1)
                )));
    }
}
