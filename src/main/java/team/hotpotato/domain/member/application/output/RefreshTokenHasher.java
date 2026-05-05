package team.hotpotato.domain.member.application.output;

public interface RefreshTokenHasher {
    String hash(String rawRefreshToken);
}
