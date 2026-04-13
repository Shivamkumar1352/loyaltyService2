package com.loyaltyService.api_gateway.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced
    @Qualifier("loadBalancedWebClientBuilder")
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    @Qualifier("walletHealthWebClient")
    public WebClient walletHealthWebClient(
            @Qualifier("loadBalancedWebClientBuilder") WebClient.Builder builder
    ) {
        return builder
                .baseUrl("lb://wallet-service")
                .build();
    }
}
