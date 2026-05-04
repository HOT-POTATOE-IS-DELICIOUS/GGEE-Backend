package team.hotpotato.domain.protect.application.usecase.indexing;

public record IndexProtectResult(
        Long protectId,
        Long indexingJobId
) {
}
