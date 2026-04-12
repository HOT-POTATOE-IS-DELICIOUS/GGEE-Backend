package team.hotpotato.security;

public final class SecurityPaths {
    public static final String[] PUBLIC_PATHS = {
            "/auth/register",
            "/auth/login",
            "/auth/refresh",
            "/actuator/health",
            "/actuator/info"
    };

    private SecurityPaths() {
    }
}
