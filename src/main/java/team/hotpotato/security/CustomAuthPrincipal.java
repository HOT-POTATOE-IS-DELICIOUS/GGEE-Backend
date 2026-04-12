package team.hotpotato.security;

import team.hotpotato.domain.member.domain.Role;

public record CustomAuthPrincipal(
        Long userId,
        Role role,
        String sessionId
) {
}
