package com.example.GitHubRepoSearch.dto;

import jakarta.validation.constraints.NotBlank;

public class SearchRequest {

    @NotBlank(message = "Search query is required")
    private String query;

    private String language;

    private String sort;

    public SearchRequest() {
    }

    public SearchRequest(String query, String language, String sort) {
        this.query = query;
        this.language = language;
        this.sort = sort;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }
}
