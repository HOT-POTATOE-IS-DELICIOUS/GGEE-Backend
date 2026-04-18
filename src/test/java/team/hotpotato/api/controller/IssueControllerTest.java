package team.hotpotato.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import team.hotpotato.GgeeBackendApplication;
import team.hotpotato.domain.issue.application.dto.IssueGraphReadCommand;
import team.hotpotato.domain.issue.application.dto.IssueGraphReadResult;
import team.hotpotato.domain.issue.application.input.IssueGraphRead;
import team.hotpotato.domain.issue.domain.IssueConnection;
import team.hotpotato.domain.issue.domain.IssueNode;
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
import static org.mockito.Mockito.when;

@SpringBootTest(classes = GgeeBackendApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("이슈 계통도 API 통합 테스트")
class IssueControllerTest {

    private WebTestClient webTestClient;

    @LocalServerPort
    private int port;

    @MockitoBean
    private IssueGraphRead issueGraphRead;

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
    @DisplayName("이슈 계통도 조회 요청은 command로 변환되어 응답을 반환한다")
    void getIssuesReturnsIssueGraph() {
        when(tokenResolver.resolve("Bearer valid-access-token"))
                .thenReturn(Mono.just(new AuthPrincipal(7L, Role.USER, "issue-session-id")));
        when(sessionRepository.findActiveByUserId(7L))
                .thenReturn(Mono.just(new Session(1L, 7L, "issue-session-id", "refresh-token", LocalDateTime.now().plusHours(1))));
        when(issueGraphRead.read(any(IssueGraphReadCommand.class)))
                .thenReturn(Mono.just(new IssueGraphReadResult(
                        "백종원",
                        List.of(
                                new IssueNode(
                                        "4:c39a66eb-67e6-4ff4-954a-4c9d2fbcd69a:6050",
                                        "백종원 더본코리아 가맹점 갑질 논란",
                                        "더본코리아 가맹점주들이 본사의 갑질 행위를 폭로하며...",
                                        "2024-03-15",
                                        0.82,
                                        0.12,
                                        0.73
                                )
                        ),
                        List.of(
                                new IssueConnection(
                                        "4:c39a66eb-67e6-4ff4-954a-4c9d2fbcd69a:6120",
                                        "4:c39a66eb-67e6-4ff4-954a-4c9d2fbcd69a:6050",
                                        0.847
                                )
                        )
                )));

        webTestClient.get()
                .uri("/issues")
                .header("Authorization", "Bearer valid-access-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.protect_target").isEqualTo("백종원")
                .jsonPath("$.issues[0].id").isEqualTo("4:c39a66eb-67e6-4ff4-954a-4c9d2fbcd69a:6050")
                .jsonPath("$.issues[0].date").isEqualTo("2024-03-15")
                .jsonPath("$.connections[0].source_id").isEqualTo("4:c39a66eb-67e6-4ff4-954a-4c9d2fbcd69a:6120")
                .jsonPath("$.connections[0].target_id").isEqualTo("4:c39a66eb-67e6-4ff4-954a-4c9d2fbcd69a:6050")
                .jsonPath("$.connections[0].similarity").isEqualTo(0.847);

        ArgumentCaptor<IssueGraphReadCommand> commandCaptor = ArgumentCaptor.forClass(IssueGraphReadCommand.class);
        verify(issueGraphRead).read(commandCaptor.capture());
        assertThat(commandCaptor.getValue()).isEqualTo(new IssueGraphReadCommand(7L));
    }

    @Test
    @DisplayName("인증 정보가 없으면 401을 반환한다")
    void getIssuesReturnsUnauthorizedWhenAuthorizationHeaderIsMissing() {
        webTestClient.get()
                .uri("/issues")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
