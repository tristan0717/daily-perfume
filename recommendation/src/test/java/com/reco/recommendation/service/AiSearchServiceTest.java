package com.reco.recommendation.service;

import com.reco.recommendation.domain.Perfume;
import com.reco.recommendation.dto.SearchRequest;
import com.reco.recommendation.repository.PerfumeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AiSearchServiceTest {
    private final PerfumeRepository repository = mock(PerfumeRepository.class);
    private final NoteTranslations translations = mock(NoteTranslations.class);
    private final NoteImageResolver images = mock(NoteImageResolver.class);
    private final PerfumeService perfumes = new PerfumeService(repository, images, translations);
    private final RestTemplate http = new RestTemplate();
    private final AiSearchService search = new AiSearchService(http, repository, perfumes, translations,
        "http://vector/api/vector-search", "https://provider/generate", "test-key");
    private final JsonMapper json = JsonMapper.builder().build();
    private Perfume product() {
        return Perfume.builder().id(1L).brand("DB brand").name("DB name")
            .category("floral").notes("{\"top\":[\"Rose\"],\"base\":[\"Musk\"]}").imageUrl("db.jpg").build();
    }
    @Test void modelCannotReplaceDatabaseFacts() {
        var result = search.validateAnswer(json.readTree("{\"recommendations\":[{\"id\":1,\"name\":\"fake\",\"imageUrl\":\"evil\",\"description\":\"추천 이유\"}]}"), List.of(product()));
        assertEquals("DB name", result.recommendations().get(0).name());
        assertEquals("db.jpg", result.recommendations().get(0).imageUrl());
        assertEquals("Rose", result.recommendations().get(0).topNotes().get(0).note());
    }
    @Test void foreignAndDuplicateIdsAreRejected() {
        for (String items : List.of("{\"id\":9,\"description\":\"x\"}", "{\"id\":1,\"description\":\"x\"},{\"id\":1,\"description\":\"y\"}")) {
            assertThrows(IllegalArgumentException.class, () -> search.validateAnswer(json.readTree("{\"recommendations\":["+items+"]}"), List.of(product())));
        }
    }
    @Test void malformedOptionalRecipeDoesNotBreakProducts() {
        var result = search.validateAnswer(json.readTree("{\"recommendations\":[{\"id\":1,\"description\":\"x\"}],\"custom_perfume\":{\"name\":\"x\",\"topNotes\":[\"Unknown\"],\"middleNotes\":[],\"baseNotes\":[],\"description\":\"x\"}}"), List.of(product()));
        assertNull(result.custom_perfume());
        assertEquals(1, result.recommendations().size());
    }
    @Test void providerFailureFallsBackToVerifiedCandidates() {
        var server = MockRestServiceServer.bindTo(http).build();
        server.expect(requestTo("http://vector/api/vector-search")).andRespond(withSuccess("{\"results\":[{\"id\":1},{\"id\":999}]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://provider/generate")).andExpect(header("x-goog-api-key", "test-key")).andRespond(withServerError());
        when(repository.findAllById(any())).thenReturn(List.of(product()));
        var result = search.search(new SearchRequest("rose", List.of()));
        assertTrue(result.fallback());
        assertEquals(List.of(1L), result.recommendations().stream().map(p -> p.id()).toList());
        server.verify();
    }
    @Test void excludedNoteIsFilteredBeforeCallingProvider() {
        var server = MockRestServiceServer.bindTo(http).build();
        server.expect(requestTo("http://vector/api/vector-search")).andRespond(withSuccess("{\"results\":[{\"id\":1}]}", MediaType.APPLICATION_JSON));
        when(repository.findAllById(any())).thenReturn(List.of(product()));
        assertTrue(search.search(new SearchRequest("rose", List.of("musk"))).recommendations().isEmpty());
        server.verify();
    }
}
