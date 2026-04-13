package com.loyaltyService.api_gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WalletAvailabilityFilterTest {

    @Test
    void nonKycApprovalRequestSkipsWalletCheck() {
        AtomicBoolean walletChecked = new AtomicBoolean(false);
        ExchangeFunction exchangeFunction = request -> {
            walletChecked.set(true);
            return Mono.just(ClientResponse.create(HttpStatus.OK).build());
        };

        WalletAvailabilityFilter filterFactory = new WalletAvailabilityFilter(
                WebClient.builder().exchangeFunction(exchangeFunction).build(),
                1000
        );
        GatewayFilter filter = filterFactory.apply(new WalletAvailabilityFilter.Config());

        RecordingChain chain = new RecordingChain();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/users/profile").build()
        );

        filter.filter(exchange, chain).block();

        assertFalse(walletChecked.get());
        assertTrue(chain.called.get());
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    void kycApprovalPassesWhenWalletServiceResponds() {
        ExchangeFunction exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.OK).build());

        WalletAvailabilityFilter filterFactory = new WalletAvailabilityFilter(
                WebClient.builder().exchangeFunction(exchangeFunction).build(),
                1000
        );
        GatewayFilter filter = filterFactory.apply(new WalletAvailabilityFilter.Config());

        RecordingChain chain = new RecordingChain();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/kyc/123/approve").build()
        );

        filter.filter(exchange, chain).block();

        assertTrue(chain.called.get());
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    void kycApprovalReturns503WhenWalletServiceUnavailable() {
        ExchangeFunction exchangeFunction = request -> Mono.error(new RuntimeException("wallet-service down"));

        WalletAvailabilityFilter filterFactory = new WalletAvailabilityFilter(
                WebClient.builder().exchangeFunction(exchangeFunction).build(),
                1000
        );
        GatewayFilter filter = filterFactory.apply(new WalletAvailabilityFilter.Config());

        RecordingChain chain = new RecordingChain();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/kyc/123/approve").build()
        );

        filter.filter(exchange, chain).block();

        assertFalse(chain.called.get());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
        assertTrue(exchange.getResponse().getBodyAsString().block().contains("KYC approval requires wallet availability"));
    }

    private static class RecordingChain implements GatewayFilterChain {
        private final AtomicBoolean called = new AtomicBoolean(false);

        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            called.set(true);
            return Mono.empty();
        }
    }
}
