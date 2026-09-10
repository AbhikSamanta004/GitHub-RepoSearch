package com.example.GitHubRepoSearch.controller;

import com.example.GitHubRepoSearch.dto.RepositoryListResponse;
import com.example.GitHubRepoSearch.dto.SearchRequest;
import com.example.GitHubRepoSearch.dto.SearchResponse;
import com.example.GitHubRepoSearch.service.GitHubService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/github")
public class GitHubController {

    private final GitHubService gitHubService;

    public GitHubController(GitHubService gitHubService) {
        this.gitHubService = gitHubService;
    }

    @PostMapping("/search")
    public ResponseEntity<SearchResponse> searchRepositories(@Valid @RequestBody SearchRequest searchRequest) {
        SearchResponse response = gitHubService.searchAndSaveRepositories(searchRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/repositories")
    public ResponseEntity<RepositoryListResponse> getStoredRepositories(
            @RequestParam(required = false) String language,
            @RequestParam(required = false) Integer minStars,
            @RequestParam(required = false, defaultValue = "stars") String sort) {

        RepositoryListResponse response = gitHubService.getStoredRepositories(language, minStars, sort);
        return ResponseEntity.ok(response);
    }
}
