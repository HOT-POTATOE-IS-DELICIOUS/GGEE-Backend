package team.hotpotato.application.dto.model;

public record AuthPrincipal(
        Long userId,
        String role
) {
}
