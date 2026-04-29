package com.reco.recommendation.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/perfumes")
public class PerfumeApiController {


    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();
    private final JdbcTemplate jdbcTemplate;

    public PerfumeApiController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchPerfumes(@RequestParam("keyword") String keyword) {
        try {
            // 1. 파이썬 FastAPI(FAISS) 서버에 20개 추출 요청 (UriComponentsBuilder.fromUriString 사용)
            String pythonUrl = UriComponentsBuilder.fromUriString("http://localhost:8000/api/vector-search")
                    .queryParam("q", keyword)
                    .toUriString();

            ResponseEntity<Map> pythonResponse = restTemplate.getForEntity(pythonUrl, Map.class);
            List<Map<String, Object>> top20Perfumes = (List<Map<String, Object>>) pythonResponse.getBody().get("results");

            // 2. 한글 매핑 데이터 로드
            List<Map<String, Object>> mappingData = jdbcTemplate.queryForList("SELECT * FROM note_mapping");
            Map<String, String> noteMap = mappingData.stream()
                    .collect(Collectors.toMap(
                            row -> row.get("note_eng").toString().trim().toLowerCase(),
                            row -> row.get("note_kor").toString().trim(),
                            (existing, replacement) -> existing
                    ));

            // 3. 파이썬이 준 20개 데이터 포맷팅
            StringBuilder dbData = new StringBuilder();
            for (Map<String, Object> row : top20Perfumes) {
                dbData.append("Brand: ").append(row.get("brand"))
                        .append(", Name: ").append(row.get("name"))
                        .append(", Category: ").append(row.get("category"))
                        .append(", Notes: ").append(row.get("notes"))
                        .append(", ImageUrl: ").append(row.get("image_url"))
                        .append("\n");
            }

            // 4. Gemini 프롬프트 (커스텀 향수도 탑/미들/베이스로 분리)
            String prompt = "너는 세계 최고의 향수 소믈리에야.\n" +
                    "아래의 [데이터베이스 향수 목록] 안에서 사용자의 요청(\"" + keyword + "\")에 가장 잘 맞는 향수 10개를 골라줘.\n\n" +
                    "[데이터베이스 향수 목록]\n" +
                    dbData.toString() + "\n\n" +
                    "또한, 추천한 10개 향수의 노트를 조합해 사용자를 위한 '단 하나의 상상 속 향수(Custom Perfume)'도 만들어줘.\n" +
                    "반드시 아래의 JSON 객체 형식으로만 대답해. 마크다운 백틱(```)은 절대 금지야.\n" +
                    "{\n" +
                    "  \"recommendations\": [\n" +
                    "    {\n" +
                    "      \"brand\": \"브랜드명\",\n" +
                    "      \"name\": \"향수 이름\",\n" +
                    "      \"imageUrl\": \"DB의 ImageUrl 값\",\n" +
                    "      \"category\": \"향 계열\",\n" +
                    "      \"topNotes\": [\"Note1\"], // 반드시 영어 원문으로\n" +
                    "      \"middleNotes\": [\"Note2\"], // 반드시 영어 원문으로\n" +
                    "      \"baseNotes\": [\"Note3\"], // 반드시 영어 원문으로\n" +
                    "      \"description\": \"추천 이유\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"custom_perfume\": {\n" +
                    "    \"name\": \"창조한 향수 이름\",\n" +
                    "    \"topNotes\": [\"탑 노트1\"], // 반드시 한글로 작성\n" +
                    "    \"middleNotes\": [\"미들 노트1\"], // 반드시 한글로 작성\n" +
                    "    \"baseNotes\": [\"베이스 노트1\"], // 반드시 한글로 작성\n" +
                    "    \"description\": \"이 조합이 어떤 특별한 향을 낼지 설명해줘\"\n" +
                    "  }\n" +
                    "}";

            // 5. Gemini API 호출
            URI uri = UriComponentsBuilder.fromUriString("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent")
                    .queryParam("key", apiKey.trim()).build().toUri();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
            requestBody.put("generationConfig", Map.of("temperature", 0.2, "response_mime_type", "application/json"));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(uri, entity, Map.class);

            // 6. 응답 추출 (🔥 JSON 500 파싱 에러 완벽 차단 방어막 추가)
            Map<String, Object> responseBody = response.getBody();
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> partsRes = (List<Map<String, Object>>) content.get("parts");

            String jsonResultString = (String) partsRes.get(0).get("text");
            // AI가 무시하고 마크다운 백틱(```json)을 붙여 보내더라도 강제로 제거합니다.
            jsonResultString = jsonResultString.replaceAll("```json", "").replaceAll("```", "").trim();

            // 7. JSON 파싱 및 데이터 매핑
            Map<String, Object> rawResult = mapper.readValue(jsonResultString, Map.class);
            List<Map<String, Object>> perfList = (List<Map<String, Object>>) rawResult.get("recommendations");

            for (Map<String, Object> p : perfList) {
                p.put("topNotes", mapToKorean(p.get("topNotes"), noteMap));
                p.put("middleNotes", mapToKorean(p.get("middleNotes"), noteMap));
                p.put("baseNotes", mapToKorean(p.get("baseNotes"), noteMap));
            }

            return ResponseEntity.ok(rawResult);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "오류 발생: " + e.getMessage()));
        }
    }

    // 영문 노트를 받아 {eng: "Lemon", kor: "레몬"} 형태의 리스트로 바꿔주는 헬퍼 메소드
    private List<Map<String, String>> mapToKorean(Object notesObj, Map<String, String> noteMap) {
        if (notesObj == null) return new ArrayList<>(); // null 방어벽
        List<String> engNotes = (List<String>) notesObj;
        List<Map<String, String>> result = new ArrayList<>();
        for (String eng : engNotes) {
            Map<String, String> map = new HashMap<>();
            map.put("eng", eng);
            map.put("kor", noteMap.getOrDefault(eng.trim().toLowerCase(), eng));
            result.add(map);
        }
        return result;
    }
}