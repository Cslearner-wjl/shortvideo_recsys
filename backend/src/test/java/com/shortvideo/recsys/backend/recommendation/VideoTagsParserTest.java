package com.shortvideo.recsys.backend.recommendation;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class VideoTagsParserTest {
    @Test
    void parseTags_shouldHandleJsonArrayAndCsv() {
        VideoTagsParser parser = new VideoTagsParser(new ObjectMapper());

        Assertions.assertEquals(List.of("a", "b"), parser.parseTags("[\"a\",\"b\"]"));
        Assertions.assertEquals(List.of("a", "b"), parser.parseTags("a,b"));
        Assertions.assertEquals(List.of(), parser.parseTags("   "));
        Assertions.assertEquals(List.of(), parser.parseTags("null"));
        Assertions.assertEquals(List.of("single"), parser.parseTags("single"));
    }
}

