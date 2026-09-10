package com.reco.recommendation.service;

import java.util.ArrayDeque;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Global per-instance quota; cannot be bypassed by spoofing forwarding headers. */
@Component
public class SearchLimiter {
    private final ArrayDeque<Long> calls = new ArrayDeque<>();
    private final int limit;
    public SearchLimiter(@Value("${ai.requests-per-minute:30}") int limit) {
        if (limit < 1) throw new IllegalArgumentException("Search quota must be positive");
        this.limit = limit;
    }
    public synchronized void acquire() {
        long now = System.nanoTime();
        while (!calls.isEmpty() && now - calls.peekFirst() >= 60_000_000_000L) calls.removeFirst();
        if (calls.size() >= limit) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "검색 요청이 많습니다. 1분 후 다시 시도해 주세요.");
        calls.addLast(now);
    }
}
