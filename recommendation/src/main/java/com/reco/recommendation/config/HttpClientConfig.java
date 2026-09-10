package com.reco.recommendation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class HttpClientConfig {
    @Bean
    RestTemplate restTemplate(@Value("${ai.connect-timeout-ms:3000}") int connect,
                              @Value("${ai.read-timeout-ms:25000}") int read) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connect);
        factory.setReadTimeout(read);
        return new RestTemplate(factory);
    }
}
