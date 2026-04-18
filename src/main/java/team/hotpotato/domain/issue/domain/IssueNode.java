package team.hotpotato.domain.issue.domain;

public record IssueNode(
        String id,
        String title,
        String summary,
        String date,
        double criticism,
        double support,
        double interest
) {
}
