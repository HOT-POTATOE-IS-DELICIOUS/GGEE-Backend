package team.hotpotato.application.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import team.hotpotato.domain.issue.application.output.IssueGraphSource;
import team.hotpotato.domain.issue.application.query.read.IssueGraphReadCommand;
import team.hotpotato.domain.issue.application.query.read.IssueGraphReadQuery;
import team.hotpotato.domain.issue.domain.IssueConnection;
import team.hotpotato.domain.issue.domain.IssueGraph;
import team.hotpotato.domain.issue.domain.IssueNode;
import team.hotpotato.domain.protect.application.input.GetProtectByUserId;
import team.hotpotato.domain.protect.domain.Protect;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("이슈 계통도 조회 쿼리 단위 테스트")
class IssueGraphReadQueryTest {

    @Test
    @DisplayName("로그인한 사용자의 보호 대상 정보로 이슈 계통도를 조회하고 정규화한다")
    void readNormalizesIssueGraph() {
        GetProtectByUserId getProtectByUserId = userId -> Mono.just(
                new Protect(42L, 7L, "백종원", "더본코리아")
        );
        String[] capturedArguments = new String[2];
        IssueGraphSource source = (protectTarget, protectTargetInfo) -> {
            capturedArguments[0] = protectTarget;
            capturedArguments[1] = protectTargetInfo;
            return Mono.just(new IssueGraph(
                "백종원",
                List.of(
                        new IssueNode("latest", "최신 이슈", "설명", "2024-09-01", 0.76, 0.18, 0.69),
                        new IssueNode("older", "과거 이슈", "설명", "2024-03-15", 0.82, 0.12, 0.73)
                ),
                List.of(new IssueConnection("older", "latest", 0.847))
            ));
        };
        IssueGraphReadQuery query = new IssueGraphReadQuery(source, getProtectByUserId);

        StepVerifier.create(query.read(new IssueGraphReadCommand(7L)))
                .assertNext(result -> {
                    assertThat(capturedArguments[0]).isEqualTo("백종원");
                    assertThat(capturedArguments[1]).isEqualTo("더본코리아");
                    assertThat(result.protectTarget()).isEqualTo("백종원");
                    assertThat(result.issues()).extracting(IssueNode::id)
                            .containsExactly("older", "latest");
                    assertThat(result.connections())
                            .containsExactly(new IssueConnection("latest", "older", 0.847));
                })
                .verifyComplete();
    }
}
