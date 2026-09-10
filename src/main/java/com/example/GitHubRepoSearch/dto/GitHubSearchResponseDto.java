package com.example.GitHubRepoSearch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubSearchResponseDto {

    @JsonProperty("total_count")
    private Integer totalCount;

    @JsonProperty("items")
    private List<GitHubRepositoryDto> items = new ArrayList<>();

    public GitHubSearchResponseDto() {
    }

    public GitHubSearchResponseDto(Integer totalCount, List<GitHubRepositoryDto> items) {
        this.totalCount = totalCount;
        this.items = items;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public List<GitHubRepositoryDto> getItems() {
        return items;
    }

    public void setItems(List<GitHubRepositoryDto> items) {
        this.items = items;
    }
}
