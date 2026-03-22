package com.ecommerce.catalog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Provides a shared {@link RestTemplate} bean for inter-service HTTP calls.
 * Configured with sensible timeouts to avoid blocking on slow/unavailable services.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000); // 3 seconds to establish connection
        factory.setReadTimeout(5_000);    // 5 seconds to read response
        return new RestTemplate(factory);
    }
}
