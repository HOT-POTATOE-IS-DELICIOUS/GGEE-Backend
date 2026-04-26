package team.hotpotato.domain.reaction.infrastructure.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("WebClient 인스턴스 생성 오버헤드 측정")
class WebClientBuildOverheadTest {

    private static final int WARMUP = 100;
    private static final int ITERATIONS = 1000;

    @Test
    @DisplayName("[수정 전] 매 요청마다 WebClient.build() 호출 시 오버헤드 측정")
    void before_measureWebClientBuildOverhead() {
        WebClient.Builder builder = WebClient.builder().baseUrl("http://localhost:8080");

        // 워밍업
        for (int i = 0; i < WARMUP; i++) {
            builder.build();
        }

        long start = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            builder.build();
        }
        long elapsed = System.nanoTime() - start;
        long elapsedMs = elapsed / 1_000_000;
        double perCallUs = (double) elapsed / ITERATIONS / 1_000;

        System.out.printf("[수정 전] WebClient.build() %d회: 총 %dms, 회당 %.2fμs%n",
                ITERATIONS, elapsedMs, perCallUs);

        assertTrue(elapsedMs >= 0);
    }

    @Test
    @DisplayName("[수정 후] WebClient 재사용 시 오버헤드 측정")
    void after_measureWebClientReuseOverhead() {
        WebClient webClient = WebClient.builder().baseUrl("http://localhost:8080").build();

        // 워밍업
        for (int i = 0; i < WARMUP; i++) {
            webClient.get();
        }

        long start = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            webClient.get();
        }
        long elapsed = System.nanoTime() - start;
        long elapsedMs = elapsed / 1_000_000;
        double perCallUs = (double) elapsed / ITERATIONS / 1_000;

        System.out.printf("[수정 후] WebClient 재사용 %d회: 총 %dms, 회당 %.3fμs%n",
                ITERATIONS, elapsedMs, perCallUs);

        assertTrue(elapsedMs >= 0);
    }

    @Test
    @DisplayName("build() vs 재사용 오버헤드 배율 비교")
    void compare_buildVsReuse() {
        WebClient.Builder builder = WebClient.builder().baseUrl("http://localhost:8080");
        WebClient reusedClient = builder.build();

        // 워밍업
        for (int i = 0; i < WARMUP; i++) {
            builder.build();
            reusedClient.get();
        }

        long buildStart = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            builder.build();
        }
        long buildNs = System.nanoTime() - buildStart;

        long reuseStart = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            reusedClient.get();
        }
        long reuseNs = System.nanoTime() - reuseStart;

        double ratio = (double) buildNs / reuseNs;
        double buildPerCallUs = (double) buildNs / ITERATIONS / 1_000;
        double reusePerCallUs = (double) reuseNs / ITERATIONS / 1_000;

        System.out.printf("%n=== WebClient 오버헤드 비교 결과 ===%n");
        System.out.printf("build()  1회: %.2fμs%n", buildPerCallUs);
        System.out.printf("reuse()  1회: %.3fμs%n", reusePerCallUs);
        System.out.printf("build/reuse 배율: %.1fx%n", ratio);
        System.out.printf("1,000 RPS 기준 초당 낭비: %.1fms%n", buildPerCallUs);

        assertTrue(ratio > 1.0, "build()가 재사용보다 느려야 한다");
    }
}
