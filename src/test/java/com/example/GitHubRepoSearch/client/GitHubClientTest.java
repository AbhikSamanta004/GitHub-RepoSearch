package com.example.GitHubRepoSearch.client;

import com.example.GitHubRepoSearch.config.AppConfig;
import com.example.GitHubRepoSearch.dto.GitHubSearchResponseDto;
import com.example.GitHubRepoSearch.exception.GitHubApiException;
import com.example.GitHubRepoSearch.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(GitHubClient.class)
@Import(AppConfig.class)
@ActiveProfiles("test")
class GitHubClientTest {

    @Autowired
    private GitHubClient gitHubClient;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    @DisplayName("Should successfully fetch repositories from GitHub API")
    void testSearchRepositoriesSuccess() {
        String jsonResponse = """
                {
                  "total_count": 1,
                  "incomplete_results": false,
                  "items": [
                    {
                      "id": 123456,
                      "name": "spring-boot-example",
                      "description": "An example repository for Spring Boot",
                      "owner": { "login": "user123" },
                      "language": "Java",
                      "stargazers_count": 450,
                      "forks_count": 120,
                      "updated_at": "2024-01-01T12:00:00Z"
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("https://api.github.com/search/repositories?q=spring%20boot%20language:Java&sort=stars&order=desc"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        GitHubSearchResponseDto result = gitHubClient.searchRepositories("spring boot", "Java", "stars");

        mockServer.verify();
        assertThat(result).isNotNull();
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getId()).isEqualTo(123456L);
        assertThat(result.getItems().get(0).getOwner().getLogin()).isEqualTo("user123");
        assertThat(result.getItems().get(0).getStars()).isEqualTo(450);
    }

    @Test
    @DisplayName("Should throw RateLimitExceededException when GitHub returns HTTP 403 Forbidden")
    void testRateLimitExceeded() {
        mockServer.expect(requestTo("https://api.github.com/search/repositories?q=test"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).body("API rate limit exceeded"));

        assertThatThrownBy(() -> gitHubClient.searchRepositories("test", null, null))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("rate limit exceeded");

        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw GitHubApiException on HTTP 500 server error")
    void testServerError() {
        mockServer.expect(requestTo("https://api.github.com/search/repositories?q=test"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> gitHubClient.searchRepositories("test", null, null))
                .isInstanceOf(GitHubApiException.class)
                .hasMessageContaining("GitHub API server error");

        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when query is blank")
    void testBlankQuery() {
        assertThatThrownBy(() -> gitHubClient.searchRepositories("  ", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Search query cannot be empty");
    }
}
