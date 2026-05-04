package team.hotpotato.domain.protect.application.dto;

public record ProtectTargetIndexingPublishCommand(
        Long jobId,
        String keyword,
        String protectTargetInfo
) {
}
