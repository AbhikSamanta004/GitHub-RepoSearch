package com.example.GitHubRepoSearch.controller;

import com.example.GitHubRepoSearch.dto.RepositoryDto;
import com.example.GitHubRepoSearch.dto.RepositoryListResponse;
import com.example.GitHubRepoSearch.dto.SearchResponse;
import com.example.GitHubRepoSearch.exception.RateLimitExceededException;
import com.example.GitHubRepoSearch.service.GitHubService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GitHubController.class)
class GitHubControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GitHubService gitHubService;

    @Test
    @DisplayName("POST /api/github/search with valid request should return 200 OK")
    void testSearchRepositoriesSuccess() throws Exception {
        Instant now = Instant.parse("2024-01-01T12:00:00Z");
        RepositoryDto dto = new RepositoryDto(123456L, "spring-boot-example", "An example repository for Spring Boot", "user123", "Java", 450, 120, now);
        SearchResponse searchResponse = new SearchResponse("Repositories fetched and saved successfully", List.of(dto));

        when(gitHubService.searchAndSaveRepositories(any())).thenReturn(searchResponse);

        String requestBody = """
                {
                  "query": "spring boot",
                  "language": "Java",
                  "sort": "stars"
                }
                """;

        mockMvc.perform(post("/api/github/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Repositories fetched and saved successfully")))
                .andExpect(jsonPath("$.repositories", hasSize(1)))
                .andExpect(jsonPath("$.repositories[0].id", is(123456)))
                .andExpect(jsonPath("$.repositories[0].name", is("spring-boot-example")))
                .andExpect(jsonPath("$.repositories[0].owner", is("user123")))
                .andExpect(jsonPath("$.repositories[0].language", is("Java")))
                .andExpect(jsonPath("$.repositories[0].stars", is(450)))
                .andExpect(jsonPath("$.repositories[0].forks", is(120)))
                .andExpect(jsonPath("$.repositories[0].lastUpdated", is("2024-01-01T12:00:00Z")));
    }

    @Test
    @DisplayName("POST /api/github/search with blank query should return 400 Bad Request")
    void testSearchRepositoriesValidationError() throws Exception {
        String requestBody = """
                {
                  "query": "  ",
                  "language": "Java"
                }
                """;

        mockMvc.perform(post("/api/github/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.details[0]", containsString("Search query is required")));
    }

    @Test
    @DisplayName("GET /api/github/repositories with query parameters should return 200 OK")
    void testGetStoredRepositoriesSuccess() throws Exception {
        Instant now = Instant.parse("2024-01-01T12:00:00Z");
        RepositoryDto dto = new RepositoryDto(123456L, "spring-boot-example", "An example repository for Spring Boot", "user123", "Java", 450, 120, now);
        RepositoryListResponse response = new RepositoryListResponse(List.of(dto));

        when(gitHubService.getStoredRepositories(eq("Java"), eq(100), eq("stars"))).thenReturn(response);

        mockMvc.perform(get("/api/github/repositories")
                        .param("language", "Java")
                        .param("minStars", "100")
                        .param("sort", "stars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositories", hasSize(1)))
                .andExpect(jsonPath("$.repositories[0].id", is(123456)))
                .andExpect(jsonPath("$.repositories[0].name", is("spring-boot-example")));
    }

    @Test
    @DisplayName("Rate limit exceeded should return 429 Too Many Requests")
    void testRateLimitExceeded() throws Exception {
        when(gitHubService.searchAndSaveRepositories(any()))
                .thenThrow(new RateLimitExceededException("GitHub API rate limit exceeded"));

        String requestBody = """
                {
                  "query": "spring boot"
                }
                """;

        mockMvc.perform(post("/api/github/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status", is(429)))
                .andExpect(jsonPath("$.message", containsString("GitHub API rate limit exceeded")));
    }
}
