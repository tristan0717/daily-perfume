package com.reco.recommendation.service;

import com.reco.recommendation.domain.NoteImage;
import com.reco.recommendation.repository.NoteImageRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
public class NoteImageBootstrap implements ApplicationRunner {

    private final NoteImageRepository noteImageRepository;

    public NoteImageBootstrap(NoteImageRepository noteImageRepository) {
        this.noteImageRepository = noteImageRepository;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // classpath에서 static/note-images의 이미지 파일들을 모두 찾음 (jar 배포에서도 동작)
        var resolver = new PathMatchingResourcePatternResolver();

        scanAndUpsert(resolver.getResources("classpath:/static/note-images/*.png"));
        scanAndUpsert(resolver.getResources("classpath:/static/note-images/*.jpg"));
        scanAndUpsert(resolver.getResources("classpath:/static/note-images/*.jpeg"));
        scanAndUpsert(resolver.getResources("classpath:/static/note-images/*.webp"));

        System.out.println("[NoteImageBootstrap] note_images ready.");
    }

    private void scanAndUpsert(Resource[] resources) {
        for (Resource r : resources) {
            String filename = r.getFilename();
            if (filename == null) continue;

            // 예: "Agarwood (Oud).jpg" -> noteName: "Agarwood (Oud)"
            String noteName = stripExtension(filename).trim();
            if (noteName.isBlank()) continue;

            // URL은 브라우저에서 안전하게 열리도록 인코딩
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                    .replace("+", "%20"); // 공백은 %20로
            String url = "/note-images/" + encodedFilename;

            // 1) 원본 이름 저장
            upsert(noteName, url);

            // 2) alias: 괄호 제거 버전도 같이 저장 (Agarwood (Oud) -> Agarwood)
            String alias = noteName.replaceAll("\\s*\\(.*?\\)\\s*", "").trim();
            if (!alias.isBlank() && !alias.equalsIgnoreCase(noteName)) {
                upsert(alias, url);
            }
        }
    }

    private void upsert(String note, String url) {
        if (noteImageRepository.existsByNoteIgnoreCase(note)) return;

        noteImageRepository.save(
                NoteImage.builder()
                        .note(note)
                        .imageUrl(url)
                        .build()
        );
    }

    private String stripExtension(String name) {
        int i = name.lastIndexOf('.');
        return (i > 0) ? name.substring(0, i) : name;
    }
}