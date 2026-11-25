package com.todayfish.backend.collection.publicapi.kmi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KmiPriceResponse(
    List<PriceItem> data
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PriceItem(
        @JsonProperty("creatDate")
        String observationDate,

        @JsonProperty("ccKdfshCode")
        String speciesCode,

        @JsonProperty("sleTy")
        String priceTypeName,

        @JsonProperty("sleCode")
        String priceTypeCode,

        @JsonProperty("pc")
        BigDecimal price
    ){}
}
