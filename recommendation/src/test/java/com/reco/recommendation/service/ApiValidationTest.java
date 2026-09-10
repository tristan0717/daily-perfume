package com.reco.recommendation.service;

import com.reco.recommendation.controller.PerfumeController;
import com.reco.recommendation.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class ApiValidationTest {
    @Test void badPaginationAndBadIdReturn400() throws Exception {
        var service = mock(PerfumeService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new PerfumeController(service))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
        for (String url : new String[]{"/api/perfumes?size=100000", "/api/perfumes?page=-1", "/api/perfumes/undefined"}) {
            assertEquals(400, mvc.perform(get(url)).andReturn().getResponse().getStatus());
        }
        verifyNoInteractions(service);
    }
    @Test void expensiveSearchQuotaCannotBeExceeded() {
        var limiter = new SearchLimiter(2);
        limiter.acquire(); limiter.acquire();
        assertEquals(429, assertThrows(ResponseStatusException.class, limiter::acquire).getStatusCode().value());
    }
}
