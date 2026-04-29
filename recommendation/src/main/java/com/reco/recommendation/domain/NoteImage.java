package com.reco.recommendation.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "note_images", uniqueConstraints = @UniqueConstraint(columnNames = "note"))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class NoteImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String note;     // 예: "Agarwood (Oud)" 또는 alias "Agarwood"

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl; // 예: "/note-images/Agarwood%20(Oud).jpg"
}