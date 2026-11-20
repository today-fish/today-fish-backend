package com.todayfish.backend.collection.publicapi.at.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AtTradesResponse(Response response) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(Header header, Body body) {
    }

    public record Header(String resultCode, String resultMsg) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(Integer pageNo, Integer numOfRows, Integer totalCount, Item items) {
    }

    public record Item(List<TradeItem> item) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TradeItem(
            @JsonProperty("trd_clcln_ymd")
            String settlementDate,
            @JsonProperty("whsl_mrkt_cd")
            String marketCode,
            @JsonProperty("gds_lclsf_cd")
            String categoryCode,
            @JsonProperty("gds_mclsf_cd")
            String middleCategoryCode,
            @JsonProperty("gds_sclsf_cd")
            String subProductCode,
            @JsonProperty("unit_nm")
            String unitName,
            @JsonProperty("unit_qty")
            String unitQuantity,
            @JsonProperty("unit_tot_qty")
            String totalQuantity,
            @JsonProperty("avgprc")
            String averagePrice,
            @JsonProperty("totprc")
            String totalPrice
    ) {
    }
}
