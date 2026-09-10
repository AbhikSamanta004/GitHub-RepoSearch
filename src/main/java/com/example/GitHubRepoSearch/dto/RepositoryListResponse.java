package com.example.GitHubRepoSearch.dto;

import java.util.List;

public class RepositoryListResponse {

    private List<RepositoryDto> repositories;

    public RepositoryListResponse() {
    }

    public RepositoryListResponse(List<RepositoryDto> repositories) {
        this.repositories = repositories;
    }

    public List<RepositoryDto> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<RepositoryDto> repositories) {
        this.repositories = repositories;
    }
}
