package team.hotpotato.security;

public final class SecurityPaths {
    public static final String[] PUBLIC_PATHS = {
            "/auth/**",
            "/actuator/health",
            "/actuator/info",
            "/perf/**"
    };

    private SecurityPaths() {
    }
}
