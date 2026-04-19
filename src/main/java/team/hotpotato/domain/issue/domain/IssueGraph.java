package team.hotpotato.domain.issue.domain;

import java.util.List;

public record IssueGraph(
        String protectTarget,
        List<IssueNode> issues,
        List<IssueConnection> connections
) {
}
