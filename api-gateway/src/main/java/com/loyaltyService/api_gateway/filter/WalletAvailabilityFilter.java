package com.loyaltyService.api_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

/**
 * Blocks KYC approval requests if wallet-service cannot be reached.
 */
@Slf4j
@Component
public class WalletAvailabilityFilter extends AbstractGatewayFilterFactory<WalletAvailabilityFilter.Config> {

    private static final Set<HttpMethod> KYC_APPROVAL_METHODS = Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH);

    private final WebClient walletHealthWebClient;
    private final Duration checkTimeout;

    public WalletAvailabilityFilter(
            @Qualifier("walletHealthWebClient") WebClient walletHealthWebClient,
            @Value("${wallet.availability.check-timeout-ms:1200}") long checkTimeoutMs
    ) {
        super(Config.class);
        this.walletHealthWebClient = walletHealthWebClient;
        this.checkTimeout = Duration.ofMillis(checkTimeoutMs);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if (!isKycApprovalRequest(exchange)) {
                return chain.filter(exchange);
            }

            return walletHealthWebClient
                    .get()
                    .uri("/actuator/health")
                    .exchangeToMono(response -> Mono.just(!response.statusCode().is5xxServerError()))
                    .timeout(checkTimeout)
                    .onErrorReturn(false)
                    .flatMap(walletAvailable -> {
                        if (walletAvailable) {
                            return chain.filter(exchange);
                        }
                        log.warn("Blocking KYC approval because wallet-service is unavailable. path={}",
                                exchange.getRequest().getPath());
                        return rejectWith(exchange, HttpStatus.SERVICE_UNAVAILABLE,
                                "wallet-service is unavailable. KYC approval requires wallet availability.");
                    });
        };
    }

    private boolean isKycApprovalRequest(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();

        if (method == null || !KYC_APPROVAL_METHODS.contains(method)) {
            return false;
        }

        return path.contains("/kyc") && path.contains("/approve");
    }

    private Mono<Void> rejectWith(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format(
                "{\"success\":false,\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}",
                status.value(), status.getReasonPhrase(), message);

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {}
}
