package team.hotpotato.domain.issue.application.input;

import reactor.core.publisher.Mono;
import team.hotpotato.domain.issue.application.dto.IssueGraphReadCommand;
import team.hotpotato.domain.issue.application.dto.IssueGraphReadResult;

public interface IssueGraphRead {
    Mono<IssueGraphReadResult> read(IssueGraphReadCommand command);
}
