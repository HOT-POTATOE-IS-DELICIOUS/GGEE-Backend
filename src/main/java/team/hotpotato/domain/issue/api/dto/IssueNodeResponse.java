package team.hotpotato.domain.issue.api.dto;

public record IssueNodeResponse(
        String id,
        String title,
        String summary,
        String date,
        double criticism,
        double support,
        double interest
) {
}
