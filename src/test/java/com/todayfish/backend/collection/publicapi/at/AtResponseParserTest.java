
package com.todayfish.backend.collection.publicapi.at;

import com.todayfish.backend.collection.publicapi.at.dto.AtTradesResponse;
import com.todayfish.backend.collection.publicapi.exception.AtProviderException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class AtResponseParserTest {

    private final AtResponseParser parser =
            new AtResponseParser(JsonMapper.builder().build());

    @Test
    void 정상_응답을_거래_DTO로_변환한다() {
        String json = """
                {
                  "response": {
                    "header": {
                      "resultCode": "0",
                      "resultMsg": "정상"
                    },
                    "body": {
                      "pageNo": 1,
                      "numOfRows": 1000,
                      "totalCount": 1,
                      "items": {
                        "item": [{
                          "trd_clcln_ymd": "2026-11-02",
                          "whsl_mrkt_cd": "240004",
                          "gds_lclsf_cd": "71",
                          "gds_mclsf_cd": "06",
                          "unit_qty": "10.000",
                          "unit_tot_qty": "740.000",
                          "avgprc": "59000.000",
                          "totprc": "4366000.000"
                        }]
                      }
                    }
                  }
                }
                """;

        AtTradesResponse response = parser.parse(bytes(json));
        AtTradesResponse.TradeItem item =
                response.response().body().items().item().getFirst();

        assertAll(
                () -> assertEquals("0",
                        response.response().header().resultCode()),
                () -> assertEquals("정상",
                        response.response().header().resultMsg()),
                () -> assertEquals(1,
                        response.response().body().totalCount()),
                () -> assertEquals("2026-11-02", item.settlementDate()),
                () -> assertEquals("240004", item.marketCode()),
                () -> assertEquals("71", item.categoryCode()),
                () -> assertEquals("06", item.middleCategoryCode()),
                () -> assertEquals("10.000", item.unitQuantity()),
                () -> assertEquals("740.000", item.totalQuantity()),
                () -> assertEquals("59000.000", item.averagePrice()),
                () -> assertEquals("4366000.000", item.totalPrice())
        );
    }

    @Test
    void 전체_건수가_0이면_항목_목록이_없어도_허용한다() {
        AtTradesResponse response = parser.parse(
                bytes(successResponse(
                        "\"pageNo\":1,\"numOfRows\":2,\"totalCount\":0"
                ))
        );

        assertEquals(0, response.response().body().totalCount());
        assertNull(response.response().body().items());
    }

    @Test
    void 알_수_없는_JSON_필드는_무시한다() {
        String json = """
                {
                  "unknown": true,
                  "response": {
                    "header": {"resultCode": "0"},
                    "body": {
                      "pageNo": 1,
                      "numOfRows": 2,
                      "totalCount": 0,
                      "extra": "value"
                    }
                  }
                }
                """;

        AtTradesResponse response = parser.parse(bytes(json));

        assertEquals(0, response.response().body().totalCount());
    }

    @Test
    void 잘못된_JSON이면_원인_예외를_보존한다() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> parser.parse(bytes("{"))
        );

        assertNotNull(exception.getCause());
    }

    @Test
    void 응답_헤더가_없으면_거부한다() {
        String json = """
                {"response":{"body":{}}}
                """;

        assertThrows(
                IllegalStateException.class,
                () -> parser.parse(bytes(json))
        );
    }

    @Test
    void 제공기관_결과코드가_오류이면_전용_예외를_던진다() {
        String json = """
                {
                  "response": {
                    "header": {
                      "resultCode": "99",
                      "resultMsg": "API error"
                    }
                  }
                }
                """;

        AtProviderException exception = assertThrows(
                AtProviderException.class,
                () -> parser.parse(bytes(json))
        );

        assertEquals("99", exception.resultCode());
    }

    @Test
    void 페이지_메타데이터가_없거나_잘못되면_거부한다() {
        assertAll(
                () -> assertThrows(
                        IllegalStateException.class,
                        () -> parser.parse(bytes(successResponse(
                                "\"pageNo\":1,\"totalCount\":0"
                        )))
                ),
                () -> assertThrows(
                        IllegalStateException.class,
                        () -> parser.parse(bytes(successResponse(
                                "\"pageNo\":1,\"numOfRows\":0,\"totalCount\":0"
                        )))
                ),
                () -> assertThrows(
                        IllegalStateException.class,
                        () -> parser.parse(bytes(successResponse(
                                "\"pageNo\":1,\"numOfRows\":2,\"totalCount\":-1"
                        )))
                )
        );
    }

    @Test
    void 전체_건수가_있지만_거래_항목이_없으면_거부한다() {
        String json = successResponse(
                "\"pageNo\":1,\"numOfRows\":2,\"totalCount\":1"
        );

        assertThrows(
                IllegalStateException.class,
                () -> parser.parse(bytes(json))
        );
    }

    @Test
    void 거래_항목_수가_페이지_크기를_초과하면_거부한다() {
        String body = """
                "pageNo": 1,
                "numOfRows": 1,
                "totalCount": 2,
                "items": {
                  "item": [{}, {}]
                }
                """;

        assertThrows(
                IllegalStateException.class,
                () -> parser.parse(bytes(successResponse(body)))
        );
    }

    private static byte[] bytes(String json) {
        return json.getBytes(StandardCharsets.UTF_8);
    }

    private static String successResponse(String bodyFields) {
        return """
                {
                  "response": {
                    "header": {"resultCode": "0"},
                    "body": {
                """
                + bodyFields
                + """
                }
                }
                }
                """;
    }
}
