package com.example.GitHubRepoSearch.dto;

import java.util.List;

public class SearchResponse {

    private String message;
    private List<RepositoryDto> repositories;

    public SearchResponse() {
    }

    public SearchResponse(String message, List<RepositoryDto> repositories) {
        this.message = message;
        this.repositories = repositories;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<RepositoryDto> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<RepositoryDto> repositories) {
        this.repositories = repositories;
    }
}
