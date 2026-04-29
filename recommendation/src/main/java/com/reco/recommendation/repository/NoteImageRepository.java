package com.reco.recommendation.repository;

import com.reco.recommendation.domain.NoteImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NoteImageRepository extends JpaRepository<NoteImage, Long> {
    Optional<NoteImage> findByNoteIgnoreCase(String note);
    boolean existsByNoteIgnoreCase(String note);
}