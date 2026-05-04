package team.hotpotato.domain.protect.domain;

import java.util.Objects;

public record Protect(
        Long id,
        Long userId,
        String target,
        String info
) {
    public Protect {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(info, "info must not be null");
    }
}
