package team.hotpotato.domain.member.infrastructure.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RefreshTokenHasherAdapter 단위 테스트")
class RefreshTokenHasherAdapterTest {

    private final RefreshTokenHasherAdapter hasher = new RefreshTokenHasherAdapter();

    @Test
    @DisplayName("SHA-256 해시는 64자 hex 문자열을 반환한다")
    void hashReturns64CharHexString() {
        String result = hasher.hash("some-refresh-token");
        assertThat(result).hasSize(64);
        assertThat(result).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("동일한 입력은 항상 동일한 해시를 반환한다")
    void hashIsDeterministic() {
        String token = "deterministic-refresh-token";
        assertThat(hasher.hash(token)).isEqualTo(hasher.hash(token));
    }

    @Test
    @DisplayName("다른 입력은 다른 해시를 반환한다")
    void differentInputsProduceDifferentHashes() {
        assertThat(hasher.hash("token-a")).isNotEqualTo(hasher.hash("token-b"));
    }

    @Test
    @DisplayName("알려진 SHA-256 해시값과 일치한다")
    void hashMatchesKnownSha256Value() {
        // SHA-256("abc") = ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad
        String result = hasher.hash("abc");
        assertThat(result).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        assertThat(result).hasSize(64);
    }
}
