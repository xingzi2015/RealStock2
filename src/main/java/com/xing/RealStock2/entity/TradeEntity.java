package com.xing.RealStock2.entity;

import com.xing.RealStock2.notify.TradeNotify;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Data
@Builder
public class TradeEntity implements Comparable<TradeEntity>{

    private final String uuid= UUID.randomUUID().toString();

    private String stockCode;
    private String userId;
    private TradeSideEnum tradeSide;
    private Integer price;
    private volatile long amount;
    private TradeNotify tradeNotify;
    private boolean activeBuy =false;

    public synchronized boolean minusAmount(long amount){
        if(this.amount<amount){
            return false;
        }
        this.amount-=amount;
        return true;
    }

    @Override
    public int compareTo(TradeEntity tradeEntity) {
        return price.compareTo(tradeEntity.getPrice());
    }
}
