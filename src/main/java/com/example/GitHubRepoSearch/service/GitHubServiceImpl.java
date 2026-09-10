package com.example.GitHubRepoSearch.service;

import com.example.GitHubRepoSearch.client.GitHubClient;
import com.example.GitHubRepoSearch.dto.GitHubRepositoryDto;
import com.example.GitHubRepoSearch.dto.GitHubSearchResponseDto;
import com.example.GitHubRepoSearch.dto.RepositoryDto;
import com.example.GitHubRepoSearch.dto.RepositoryListResponse;
import com.example.GitHubRepoSearch.dto.SearchRequest;
import com.example.GitHubRepoSearch.dto.SearchResponse;
import com.example.GitHubRepoSearch.entity.RepositoryEntity;
import com.example.GitHubRepoSearch.repository.RepositoryJpaRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GitHubServiceImpl implements GitHubService {

    private static final Logger log = LoggerFactory.getLogger(GitHubServiceImpl.class);

    private final GitHubClient gitHubClient;
    private final RepositoryJpaRepository repositoryJpaRepository;

    public GitHubServiceImpl(GitHubClient gitHubClient, RepositoryJpaRepository repositoryJpaRepository) {
        this.gitHubClient = gitHubClient;
        this.repositoryJpaRepository = repositoryJpaRepository;
    }

    @Override
    @Transactional
    public SearchResponse searchAndSaveRepositories(SearchRequest request) {
        log.info("Processing repository search request: query='{}', language='{}', sort='{}'",
                request.getQuery(), request.getLanguage(), request.getSort());

        GitHubSearchResponseDto searchResult = gitHubClient.searchRepositories(
                request.getQuery(), request.getLanguage(), request.getSort());

        List<GitHubRepositoryDto> items = searchResult.getItems();
        if (items == null || items.isEmpty()) {
            log.info("No repositories found for query: '{}'", request.getQuery());
            return new SearchResponse("Repositories fetched and saved successfully", new ArrayList<>());
        }

        List<RepositoryEntity> entitiesToSave = items.stream()
                .map(this::convertToEntity)
                .collect(Collectors.toList());

        List<RepositoryEntity> savedEntities = repositoryJpaRepository.saveAll(entitiesToSave);
        log.info("Successfully saved/updated {} repositories in database", savedEntities.size());

        List<RepositoryDto> dtoList = savedEntities.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return new SearchResponse("Repositories fetched and saved successfully", dtoList);
    }

    @Override
    @Transactional(readOnly = true)
    public RepositoryListResponse getStoredRepositories(String language, Integer minStars, String sort) {
        log.info("Retrieving stored repositories with filters: language='{}', minStars={}, sort='{}'",
                language, minStars, sort);

        Specification<RepositoryEntity> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (language != null && !language.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("language")),
                        language.trim().toLowerCase()
                ));
            }

            if (minStars != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("stars"),
                        minStars
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Sort sortObj = resolveSort(sort);
        List<RepositoryEntity> entities = repositoryJpaRepository.findAll(spec, sortObj);

        List<RepositoryDto> dtoList = entities.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return new RepositoryListResponse(dtoList);
    }

    private RepositoryEntity convertToEntity(GitHubRepositoryDto dto) {
        RepositoryEntity entity = new RepositoryEntity();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setOwner(dto.getOwner() != null ? dto.getOwner().getLogin() : "unknown");
        entity.setLanguage(dto.getLanguage());
        entity.setStars(dto.getStars() != null ? dto.getStars() : 0);
        entity.setForks(dto.getForks() != null ? dto.getForks() : 0);
        entity.setLastUpdated(dto.getLastUpdated());
        return entity;
    }

    private RepositoryDto convertToDto(RepositoryEntity entity) {
        return new RepositoryDto(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getOwner(),
                entity.getLanguage(),
                entity.getStars(),
                entity.getForks(),
                entity.getLastUpdated()
        );
    }

    private Sort resolveSort(String sortParam) {
        if (sortParam == null || sortParam.trim().isEmpty()) {
            return Sort.by(Sort.Direction.DESC, "stars");
        }

        String s = sortParam.trim().toLowerCase();
        switch (s) {
            case "forks":
                return Sort.by(Sort.Direction.DESC, "forks");
            case "updated":
            case "updateddate":
            case "lastupdated":
                return Sort.by(Sort.Direction.DESC, "lastUpdated");
            case "stars":
            default:
                return Sort.by(Sort.Direction.DESC, "stars");
        }
    }
}
