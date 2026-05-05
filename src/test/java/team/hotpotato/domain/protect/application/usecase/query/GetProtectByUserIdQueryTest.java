package team.hotpotato.domain.protect.application.usecase.query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import team.hotpotato.common.exception.ErrorCode;
import team.hotpotato.domain.protect.application.output.ProtectRepository;
import team.hotpotato.domain.protect.domain.Protect;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Protect 조회 유스케이스 단위 테스트")
class GetProtectByUserIdQueryTest {

    @Mock
    private ProtectRepository protectRepository;

    private GetProtectByUserIdQuery query;

    @BeforeEach
    void setUp() {
        query = new GetProtectByUserIdQuery(protectRepository);
    }

    @Test
    @DisplayName("사용자의 활성 protect가 있으면 그대로 반환한다")
    void getReturnsProtectWhenPresent() {
        Protect protect = new Protect(11L, 7L, "brand", "공식몰");
        when(protectRepository.findByUserId(7L)).thenReturn(Mono.just(protect));

        StepVerifier.create(query.get(7L))
                .expectNext(protect)
                .verifyComplete();
    }

    @Test
    @DisplayName("활성 protect가 없으면 ProtectNotFoundException을 던진다")
    void getThrowsWhenAbsent() {
        when(protectRepository.findByUserId(7L)).thenReturn(Mono.empty());

        StepVerifier.create(query.get(7L))
                .expectErrorMatches(error ->
                        error instanceof ProtectNotFoundException
                                && ((ProtectNotFoundException) error).getErrorCode() == ErrorCode.PROTECT_NOT_FOUND
                )
                .verify();
    }
}
