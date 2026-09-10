package com.example.GitHubRepoSearch.exception;

public class RateLimitExceededException extends GitHubApiException {

    public RateLimitExceededException(String message) {
        super(message, 429);
    }
}
