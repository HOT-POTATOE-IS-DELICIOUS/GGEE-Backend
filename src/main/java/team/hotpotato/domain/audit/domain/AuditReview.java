package team.hotpotato.domain.audit.domain;

import java.util.List;

public record AuditReview(
        AuditSentence sentence,
        List<String> perspectiveIds,
        List<String> perspectiveLabels,
        List<AuditSuggestion> suggestions
) {
}
