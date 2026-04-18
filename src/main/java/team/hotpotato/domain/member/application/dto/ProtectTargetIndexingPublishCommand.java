package team.hotpotato.domain.member.application.dto;

public record ProtectTargetIndexingPublishCommand(
        String jobId,
        String keyword,
        String protectTargetInfo
) {
}
