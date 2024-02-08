package com.xing.RealStock2.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

@Builder
@Getter
@ToString
public class TradeMsg {
    private String stockCode;
    private Integer intendPrice;
    private Integer transactionPrice;

    private String uuid;

    private Long amount;
    private LocalDateTime dateTime;
}
