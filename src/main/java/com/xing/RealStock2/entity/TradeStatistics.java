package com.xing.RealStock2.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TradeStatistics implements Comparable<TradeStatistics> {
    private long totalAmount;
    private double totalTransactionPrice;
    private double averagePrice;
    private LocalDateTime dateTime;


    @Override
    public int compareTo(TradeStatistics o) {
        return this.dateTime.compareTo(o.dateTime);
    }
}
