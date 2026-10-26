package com.ritech.backend.market;

import java.time.Clock;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class YahooFinanceConfig {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final Duration RETRY_DELAY = Duration.ofSeconds(2);

    @Bean
    public YahooFinanceClient yahooFinanceClient(Clock clock) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TIMEOUT);
        requestFactory.setReadTimeout(TIMEOUT);
        RestClient restClient = RestClient.builder().requestFactory(requestFactory).build();
        return new YahooFinanceClient(restClient, clock, RETRY_DELAY);
    }
}
