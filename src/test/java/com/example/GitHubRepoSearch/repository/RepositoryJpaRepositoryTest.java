package com.example.GitHubRepoSearch.repository;

import com.example.GitHubRepoSearch.entity.RepositoryEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class RepositoryJpaRepositoryTest {

    @Autowired
    private RepositoryJpaRepository repositoryJpaRepository;

    @BeforeEach
    void setUp() {
        repositoryJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("Should save repository and perform upsert on duplicate ID")
    void testSaveAndUpsert() {
        Instant now = Instant.now();
        RepositoryEntity repo1 = new RepositoryEntity(100L, "spring-boot", "Spring boot repo", "spring-projects", "Java", 50000, 20000, now);
        repositoryJpaRepository.save(repo1);

        assertThat(repositoryJpaRepository.count()).isEqualTo(1);
        RepositoryEntity found = repositoryJpaRepository.findById(100L).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getStars()).isEqualTo(50000);

        // Upsert: update details for same ID 100L
        RepositoryEntity updatedRepo1 = new RepositoryEntity(100L, "spring-boot", "Spring boot updated repo", "spring-projects", "Java", 55000, 21000, now.plusSeconds(3600));
        repositoryJpaRepository.save(updatedRepo1);

        assertThat(repositoryJpaRepository.count()).isEqualTo(1);
        RepositoryEntity updatedFound = repositoryJpaRepository.findById(100L).orElse(null);
        assertThat(updatedFound).isNotNull();
        assertThat(updatedFound.getStars()).isEqualTo(55000);
        assertThat(updatedFound.getDescription()).isEqualTo("Spring boot updated repo");
    }

    @Test
    @DisplayName("Should filter by language case-insensitively and minStars")
    void testFilteringByLanguageAndMinStars() {
        Instant now = Instant.now();
        repositoryJpaRepository.save(new RepositoryEntity(1L, "java-repo-1", "desc 1", "user1", "Java", 100, 10, now));
        repositoryJpaRepository.save(new RepositoryEntity(2L, "java-repo-2", "desc 2", "user2", "java", 50, 5, now));
        repositoryJpaRepository.save(new RepositoryEntity(3L, "python-repo", "desc 3", "user3", "Python", 300, 30, now));

        Specification<RepositoryEntity> spec = (root, query, cb) -> cb.and(
                cb.equal(cb.lower(root.get("language")), "java"),
                cb.greaterThanOrEqualTo(root.get("stars"), 80)
        );

        List<RepositoryEntity> results = repositoryJpaRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "stars"));
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("java-repo-1");
    }

    @Test
    @DisplayName("Should sort results by forks descending")
    void testSortingByForks() {
        Instant now = Instant.now();
        repositoryJpaRepository.save(new RepositoryEntity(1L, "repo-1", "desc", "user1", "Java", 100, 50, now));
        repositoryJpaRepository.save(new RepositoryEntity(2L, "repo-2", "desc", "user2", "Java", 200, 300, now));
        repositoryJpaRepository.save(new RepositoryEntity(3L, "repo-3", "desc", "user3", "Java", 300, 10, now));

        List<RepositoryEntity> sortedResults = repositoryJpaRepository.findAll(Sort.by(Sort.Direction.DESC, "forks"));

        assertThat(sortedResults).extracting(RepositoryEntity::getId).containsExactly(2L, 1L, 3L);
    }
}
