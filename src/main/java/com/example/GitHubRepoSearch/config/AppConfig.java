package com.example.GitHubRepoSearch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class AppConfig {

    @Value("${github.api.token:#{null}}")
    private String githubToken;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        RestTemplate restTemplate = new RestTemplate(requestFactory);

        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add((request, body, execution) -> {
            request.getHeaders().set(HttpHeaders.ACCEPT, "application/vnd.github+json");
            request.getHeaders().set(HttpHeaders.USER_AGENT, "Spring-Boot-GitHubRepoSearch-App");

            String tokenToUse = (githubToken != null && !githubToken.trim().isEmpty())
                    ? githubToken.trim()
                    : System.getenv("GITHUB_TOKEN");

            if (tokenToUse != null && !tokenToUse.trim().isEmpty()) {
                request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + tokenToUse.trim());
            }

            return execution.execute(request, body);
        });

        restTemplate.setInterceptors(interceptors);
        return restTemplate;
    }
}
