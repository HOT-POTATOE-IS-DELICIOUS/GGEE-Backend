package team.hotpotato.domain.issue.application.input;

import reactor.core.publisher.Mono;
import team.hotpotato.domain.issue.application.query.read.IssueGraphReadCommand;
import team.hotpotato.domain.issue.application.query.read.IssueGraphReadResult;

public interface IssueGraphRead {
    Mono<IssueGraphReadResult> read(IssueGraphReadCommand command);
}
