package com.xing.RealStock2.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class ResultEntity {
    private Boolean success;
    private String message;
    private TradeEntity matchingTrade;


    public ResultEntity(Boolean success) {
        this.success = success;
    }

    public ResultEntity(Boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public ResultEntity(Boolean success, TradeEntity matchingTrade) {
        this.success = success;
        this.matchingTrade = matchingTrade;
    }
}
