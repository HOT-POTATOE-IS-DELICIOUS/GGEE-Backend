package team.hotpotato.domain.protect.application.usecase.indexing;

public record IndexProtectCommand(
        Long userId,
        String target,
        String info
) {
}
