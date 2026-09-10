package com.reco.recommendation.service;

import com.reco.recommendation.domain.Perfume;
import com.reco.recommendation.repository.PerfumeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PerfumePersistenceTest {
    @Autowired PerfumeRepository repository;
    @Autowired PerfumeService service;
    private Perfume save(String name, String notes) {
        return repository.saveAndFlush(Perfume.builder().name(name).brand("Test").category("floral").notes(notes).build());
    }
    @Test void recommendationsMatchWholeNotesInBothSupportedStorageFormats() {
        var target = save("Target", "[\"Rose\",\"Musk\"]");
        var other = save("Other", "{\"top\":[\"Rose\"]}");
        var flat = save("Flat", "Rose, Jasmine");
        save("Rose brand name only", "Cedar");
        save("Substring", "Rosemary, Musk");
        var ids = service.getRecommendedPerfumes(target.getId()).stream().map(p -> p.id()).toList();
        assertEquals(List.of(other.getId(), flat.getId()), ids);
    }
    @Test void originalPyramidIsPreservedAndUnknownPyramidIsNotInvented() {
        var original = service.getPerfume(save("Pyramid", "{\"top\":[\"Rose\"],\"middle\":null,\"base\":[\"Musk\"]}").getId());
        assertEquals("Rose", original.topNotes().get(0).note());
        assertTrue(original.middleNotes().isEmpty());
        var plain = service.getPerfume(save("Flat", "Rose, Musk").getId());
        assertTrue(plain.topNotes().isEmpty());
        assertEquals(2, plain.noteImages().size());
    }
}
