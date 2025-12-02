package org.jl.orderprocessing.Infrastructure.config;


import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;

@Configuration
public class RestTemplateConfig {


    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {

        // 1. Configure the Apache HttpClient Connection Pool
        // This is the "Engine" that manages the physical TCP sockets.
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(200);      // Global limit: 200 open sockets
        connectionManager.setDefaultMaxPerRoute(50); // Per-host limit: 50 sockets

        // 2. Build the Apache Client
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();

        // 3. Configure the Request Factory
        // This adapts the Apache Client to be used by Spring's RestTemplate
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

        // 4. Use the Builder to assemble everything
        // ACCORDING TO DOCS: Always use the builder so you don't lose metrics/converters.
        return builder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(5))
                .requestFactory(() -> requestFactory)
                .build();
    }
}