package com.todayfish.backend.collection.publicapi.kmi;

import com.todayfish.backend.collection.publicapi.kmi.dto.KmiPriceResponse;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class KmiResponseParserTest {

    private final KmiResponseParser parser = new KmiResponseParser(JsonMapper.builder().build());

    @Test
    void 기간_응답의_복수_행과_0원_값을_그대로_읽는다() {
        KmiPriceResponse response = parser.parse(bytes("""
                {"data":[
                  {"sleTy":"산지","sleCode":"SLE03","creatDate":"2026-11-01",
                   "ccKdfshCode":"KDFSH08","pc":0,"yr":15454},
                  {"sleTy":"산지","sleCode":"SLE03","creatDate":"2026-11-02",
                   "ccKdfshCode":"KDFSH08","pc":25179},
                  {"sleTy":"산지","sleCode":"SLE03","creatDate":"2026-11-02",
                   "ccKdfshCode":"KDFSH08","pc":17931}
                ]}
                """));

        assertEquals(3, response.data().size());
        assertEquals(0, response.data().getFirst().price().intValue());
        assertEquals("2026-11-02", response.data().get(1).observationDate());
        assertEquals("2026-11-02", response.data().get(2).observationDate());
        assertEquals("KDFSH08", response.data().getFirst().speciesCode());
    }

    @Test
    void 빈_데이터_배열은_정상_응답으로_받는다() {
        assertTrue(parser.parse(bytes("{\"data\":[]}")).data().isEmpty());
    }

    @Test
    void 데이터가_null이면_응답_오류로_처리한다() {
        assertThrows(IllegalStateException.class,
                () -> parser.parse(bytes("{\"data\":null}")));
    }

    @Test
    void 가격이_null이어도_원천_관측값으로_보존한다() {
        KmiPriceResponse response = parser.parse(bytes("""
                {"data":[{"sleTy":"산지","sleCode":"SLE03","creatDate":"2026-11-01",
                  "ccKdfshCode":"KDFSH08","pc":null}]}
                """));

        assertEquals(1, response.data().size());
        assertNull(response.data().getFirst().price());
    }

    @Test
    void 잘못된_JSON은_원인을_보존한_응답_오류로_전달한다() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> parser.parse(bytes("{")));

        assertNotNull(error.getCause());
    }

    @Test
    void 데이터_배열이_없으면_응답_오류로_전달한다() {
        assertThrows(IllegalStateException.class, () -> parser.parse(bytes("{}")));
    }

    @Test
    void 데이터_배열에_null_항목이_있으면_응답_오류로_전달한다() {
        assertThrows(IllegalStateException.class,
                () -> parser.parse(bytes("{\"data\":[null]}")));
    }

    @Test
    void 데이터가_배열이_아니면_응답_오류로_처리한다() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> parser.parse(bytes("{\"data\":{}}")));

        assertNotNull(error.getCause());
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
