package team.hotpotato.domain.member.application.auth;

import team.hotpotato.domain.member.domain.Role;

public record AuthPrincipal(
        Long userId,
        Role role
) {
}
