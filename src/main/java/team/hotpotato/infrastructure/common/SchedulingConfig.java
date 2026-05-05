package team.hotpotato.infrastructure.common;

import org.springframework.context.annotation.Configuration;

/**
 * 본 프로젝트는 @Scheduled 사용 빈이 존재하지 않으므로 @EnableScheduling을 비활성화한다.
 * 스케줄링은 각 컴포넌트의 @PostConstruct + Flux.interval 라이프사이클로 관리한다.
 */
@Configuration
public class SchedulingConfig {
}
