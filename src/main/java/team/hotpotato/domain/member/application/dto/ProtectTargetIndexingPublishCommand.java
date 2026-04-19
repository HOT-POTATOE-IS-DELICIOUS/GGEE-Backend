package team.hotpotato.domain.member.application.dto;

public record ProtectTargetIndexingPublishCommand(
        Long jobId,
        String keyword,
        String protectTargetInfo
) {
}
