package org.ryanair.flight.api.config;


import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import org.ryanair.flight.api.config.property.RyanairBackEndPropertyConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

/**
 * Configuration class for WebClient setup.
 */
@Configuration
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class WebClientConfig {

    private final RyanairBackEndPropertyConfiguration propertyConfiguration;

    /**
     * Configures and provides a WebClient bean for making HTTP requests.
     *
     * @return Configured WebClient instance.
     */
    @Bean
    public WebClient webclient() {

        // Configure HTTP client options
        HttpClient httpClient = HttpClient.create()
                .tcpConfiguration(client ->
                        client.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, propertyConfiguration.getConnectTimeout())
                                .doOnConnected(conn -> conn
                                        .addHandler(new ReadTimeoutHandler(propertyConfiguration.getReadTimeout(), TimeUnit.MILLISECONDS))
                                        .addHandlerLast(new WriteTimeoutHandler(propertyConfiguration.getWriteTimeout(), TimeUnit.MILLISECONDS))));

        // Create a ReactorClientHttpConnector with the configured HTTP client
        ClientHttpConnector connector = new ReactorClientHttpConnector(httpClient.wiretap(true));

        // Configure exchange strategies for handling response body
        final int size = propertyConfiguration.getMaxInMemBufferSizeMb() * 1024 * 1024;
        final ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size))
                .build();
        return WebClient
                .builder()
                .baseUrl(propertyConfiguration.getBaseUrl())
                .clientConnector(connector)
                .exchangeStrategies(strategies)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
