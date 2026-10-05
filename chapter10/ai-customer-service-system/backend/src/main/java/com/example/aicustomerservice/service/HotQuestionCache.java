package com.example.aicustomerservice.service;

import java.util.Optional;

public interface HotQuestionCache {

    Optional<String> get(String cacheKey);

    void put(String cacheKey, String answer);
}
