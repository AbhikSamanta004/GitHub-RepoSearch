package com.example.GitHubRepoSearch.service;

import com.example.GitHubRepoSearch.client.GitHubClient;
import com.example.GitHubRepoSearch.dto.GitHubOwnerDto;
import com.example.GitHubRepoSearch.dto.GitHubRepositoryDto;
import com.example.GitHubRepoSearch.dto.GitHubSearchResponseDto;
import com.example.GitHubRepoSearch.dto.RepositoryListResponse;
import com.example.GitHubRepoSearch.dto.SearchRequest;
import com.example.GitHubRepoSearch.dto.SearchResponse;
import com.example.GitHubRepoSearch.entity.RepositoryEntity;
import com.example.GitHubRepoSearch.repository.RepositoryJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitHubServiceImplTest {

    @Mock
    private GitHubClient gitHubClient;

    @Mock
    private RepositoryJpaRepository repositoryJpaRepository;

    @InjectMocks
    private GitHubServiceImpl gitHubService;

    @Test
    @DisplayName("searchAndSaveRepositories should fetch from client, save entities, and return SearchResponse")
    void testSearchAndSaveRepositoriesSuccess() {
        SearchRequest request = new SearchRequest("spring boot", "Java", "stars");
        Instant now = Instant.parse("2024-01-01T12:00:00Z");

        GitHubRepositoryDto repoDto = new GitHubRepositoryDto(
                123456L, "spring-boot-example", "An example repository",
                new GitHubOwnerDto("user123"), "Java", 450, 120, now
        );

        GitHubSearchResponseDto responseDto = new GitHubSearchResponseDto(1, List.of(repoDto));

        when(gitHubClient.searchRepositories("spring boot", "Java", "stars")).thenReturn(responseDto);

        RepositoryEntity entity = new RepositoryEntity(
                123456L, "spring-boot-example", "An example repository",
                "user123", "Java", 450, 120, now
        );
        when(repositoryJpaRepository.saveAll(anyList())).thenReturn(List.of(entity));

        SearchResponse response = gitHubService.searchAndSaveRepositories(request);

        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("Repositories fetched and saved successfully");
        assertThat(response.getRepositories()).hasSize(1);
        assertThat(response.getRepositories().get(0).getId()).isEqualTo(123456L);
        assertThat(response.getRepositories().get(0).getOwner()).isEqualTo("user123");

        verify(gitHubClient).searchRepositories("spring boot", "Java", "stars");
        verify(repositoryJpaRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("searchAndSaveRepositories with empty results should return empty repository list")
    void testSearchAndSaveRepositoriesEmpty() {
        SearchRequest request = new SearchRequest("nonexistentquery", null, null);
        when(gitHubClient.searchRepositories("nonexistentquery", null, null))
                .thenReturn(new GitHubSearchResponseDto(0, Collections.emptyList()));

        SearchResponse response = gitHubService.searchAndSaveRepositories(request);

        assertThat(response).isNotNull();
        assertThat(response.getRepositories()).isEmpty();
    }

    @Test
    @DisplayName("getStoredRepositories should return filtered and sorted stored repositories")
    void testGetStoredRepositories() {
        Instant now = Instant.now();
        RepositoryEntity entity = new RepositoryEntity(
                1L, "java-repo", "desc", "user1", "Java", 200, 50, now
        );

        when(repositoryJpaRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(entity));

        RepositoryListResponse response = gitHubService.getStoredRepositories("Java", 100, "stars");

        assertThat(response).isNotNull();
        assertThat(response.getRepositories()).hasSize(1);
        assertThat(response.getRepositories().get(0).getName()).isEqualTo("java-repo");

        verify(repositoryJpaRepository).findAll(any(Specification.class), any(Sort.class));
    }
}
