package com.shortvideo.recsys.backend.recommendation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class VideoTagsParser {
    private final ObjectMapper objectMapper;

    public VideoTagsParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<String> parseTags(String tags) {
        if (tags == null) {
            return List.of();
        }
        String s = tags.trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) {
            return List.of();
        }

        if (s.startsWith("[")) {
            return parseJsonArray(s);
        }

        if (s.contains(",")) {
            return splitCsv(s);
        }

        return List.of(s);
    }

    private List<String> parseJsonArray(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            if (!node.isArray()) {
                return List.of();
            }
            List<String> out = new ArrayList<>();
            for (JsonNode item : node) {
                if (item != null && item.isTextual()) {
                    String v = item.asText().trim();
                    if (!v.isEmpty()) {
                        out.add(v);
                    }
                }
            }
            return out;
        } catch (Exception ignored) {
            return splitCsv(json.replace("[", "").replace("]", ""));
        }
    }

    private static List<String> splitCsv(String s) {
        return java.util.Arrays.stream(s.split(","))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .distinct()
                .toList();
    }
}
