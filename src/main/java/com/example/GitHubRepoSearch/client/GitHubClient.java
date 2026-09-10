package com.example.GitHubRepoSearch.client;

import com.example.GitHubRepoSearch.dto.GitHubSearchResponseDto;
import com.example.GitHubRepoSearch.exception.GitHubApiException;
import com.example.GitHubRepoSearch.exception.RateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class GitHubClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public GitHubClient(RestTemplate restTemplate, @Value("${github.api.base-url:https://api.github.com}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public GitHubSearchResponseDto searchRepositories(String query, String language, String sort) {
        StringBuilder qBuilder = new StringBuilder();
        if (query != null && !query.trim().isEmpty()) {
            qBuilder.append(query.trim());
        }

        if (language != null && !language.trim().isEmpty()) {
            if (qBuilder.length() > 0) {
                qBuilder.append(" ");
            }
            qBuilder.append("language:").append(language.trim());
        }

        String finalQuery = qBuilder.toString();
        if (finalQuery.isBlank()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(baseUrl + "/search/repositories")
                .queryParam("q", finalQuery);

        if (sort != null && !sort.trim().isEmpty()) {
            String sanitizedSort = sanitizeSortParam(sort);
            if (sanitizedSort != null) {
                uriBuilder.queryParam("sort", sanitizedSort);
                uriBuilder.queryParam("order", "desc");
            }
        }

        URI requestUri = uriBuilder.build().toUri();
        log.info("Fetching repositories from GitHub API: {}", requestUri);

        try {
            GitHubSearchResponseDto response = restTemplate.getForObject(requestUri, GitHubSearchResponseDto.class);
            if (response == null) {
                log.warn("Received null response from GitHub API for query: {}", finalQuery);
                return new GitHubSearchResponseDto();
            }
            return response;
        } catch (HttpClientErrorException e) {
            log.error("Client error from GitHub API: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.FORBIDDEN || e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                throw new RateLimitExceededException("GitHub API rate limit exceeded or access forbidden.");
            }
            throw new GitHubApiException("GitHub API error: " + e.getResponseBodyAsString(), e.getStatusCode().value());
        } catch (HttpServerErrorException e) {
            log.error("Server error from GitHub API: status={}", e.getStatusCode());
            throw new GitHubApiException("GitHub API server error. Please try again later.", HttpStatus.BAD_GATEWAY.value());
        } catch (ResourceAccessException e) {
            log.error("Timeout or connection error contacting GitHub API: {}", e.getMessage());
            throw new GitHubApiException("Unable to communicate with GitHub API (network timeout or connection failure).", HttpStatus.GATEWAY_TIMEOUT.value());
        } catch (Exception e) {
            log.error("Unexpected error contacting GitHub API", e);
            throw new GitHubApiException("Unexpected error occurred while calling GitHub API: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    private String sanitizeSortParam(String sort) {
        if (sort == null) return null;
        String s = sort.trim().toLowerCase();
        if ("stars".equals(s) || "forks".equals(s) || "updated".equals(s)) {
            return s;
        }
        if ("updateddate".equals(s) || "lastupdated".equals(s)) {
            return "updated";
        }
        return null;
    }
}
