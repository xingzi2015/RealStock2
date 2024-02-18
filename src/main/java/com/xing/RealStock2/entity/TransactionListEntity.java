package com.xing.RealStock2.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@Getter
@Builder
public class TransactionListEntity {
    List<TradeMsg> tradeMessages;
    Integer size;
    Boolean closeFlag;
    LocalDateTime startDate;
    LocalDateTime endDate;
    Integer minPrice;
    Integer maxPrice;
}
