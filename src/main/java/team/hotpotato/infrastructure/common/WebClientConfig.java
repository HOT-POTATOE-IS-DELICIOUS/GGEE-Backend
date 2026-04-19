package team.hotpotato.infrastructure.common;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration(proxyBeanMethods = false)
public class WebClientConfig {

    @Bean
    @ConditionalOnMissingBean(WebClient.Builder.class)
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
