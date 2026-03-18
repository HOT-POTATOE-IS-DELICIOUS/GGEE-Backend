package team.hotpotato.domain.member.application.auth;

public record AuthPrincipal(
        Long userId,
        String role
) {
}
