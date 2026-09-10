package com.example.GitHubRepoSearch.service;

import com.example.GitHubRepoSearch.dto.RepositoryListResponse;
import com.example.GitHubRepoSearch.dto.SearchRequest;
import com.example.GitHubRepoSearch.dto.SearchResponse;

public interface GitHubService {
    SearchResponse searchAndSaveRepositories(SearchRequest request);
    RepositoryListResponse getStoredRepositories(String language, Integer minStars, String sort);
}
