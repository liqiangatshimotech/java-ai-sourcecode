package com.example.aicustomerservice.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Optional;

/** 单进程热点缓存：过期淘汰 + LRU 容量上限。多副本部署需要替换成共享缓存。 */
public class InMemoryHotQuestionCache implements HotQuestionCache {
    private record Entry(String answer, Instant expiresAt) { }
    private final LinkedHashMap<String, Entry> cache = new LinkedHashMap<>(16, 0.75f, true);
    private final Clock clock;
    private final Duration ttl;
    private final int capacity;
    public InMemoryHotQuestionCache() { this(Clock.systemUTC(), Duration.ofMinutes(5), 1000); }
    public InMemoryHotQuestionCache(Clock clock, Duration ttl, int capacity) {
        if (ttl.isNegative() || ttl.isZero() || capacity < 1) throw new IllegalArgumentException("缓存 TTL 和容量必须为正");
        this.clock = clock; this.ttl = ttl; this.capacity = capacity;
    }
    @Override public synchronized Optional<String> get(String key) {
        Entry entry = cache.get(key);
        if (entry == null) return Optional.empty();
        if (!clock.instant().isBefore(entry.expiresAt())) { cache.remove(key); return Optional.empty(); }
        return Optional.of(entry.answer());
    }
    @Override public synchronized void put(String key, String answer) {
        cache.entrySet().removeIf(e -> !clock.instant().isBefore(e.getValue().expiresAt()));
        cache.put(key, new Entry(answer, clock.instant().plus(ttl)));
        while (cache.size() > capacity) cache.remove(cache.keySet().iterator().next());
    }
}
