package team.hotpotato.domain.issue.application.output;

import reactor.core.publisher.Mono;
import team.hotpotato.domain.issue.domain.IssueGraph;

public interface IssueGraphSource {
    Mono<IssueGraph> read(String protectTarget, String protectTargetInfo);
}
