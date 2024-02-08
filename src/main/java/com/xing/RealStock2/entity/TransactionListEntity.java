package com.xing.RealStock2.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class TransactionListEntity {
    List<TradeMsg> tradeMessages;
    Integer size;
    Boolean closeFlag;
}
