package team.hotpotato.domain.issue.application.query.read;

import team.hotpotato.domain.issue.domain.IssueConnection;
import team.hotpotato.domain.issue.domain.IssueNode;

import java.util.List;

public record IssueGraphReadResult(
        String protectTarget,
        List<IssueNode> issues,
        List<IssueConnection> connections
) {
}
