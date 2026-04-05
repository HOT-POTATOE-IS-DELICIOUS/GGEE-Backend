package team.hotpotato.domain.member.application.model;

import team.hotpotato.domain.member.domain.Role;

public record AuthPrincipal(
        Long userId,
        Role role,
        String sessionId
) {
}
