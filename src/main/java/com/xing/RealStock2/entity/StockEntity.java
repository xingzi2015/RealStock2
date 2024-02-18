package com.xing.RealStock2.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.xing.RealStock2.common.DailyClear;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Data
@Builder
public class StockEntity implements DailyClear {

    private String stockCode;

    private AtomicInteger nowPrice;

    private Integer lastPrice;
    private Integer maxPrice;
    private Integer minPrice;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private AtomicInteger highestPrice;

    private AtomicInteger lowestPrice;

    private List<StockEntity> stockHistoryEntities;

    @JsonIgnore
    private StockDetailEntity stockDetailEntity;


    public void refresh(){
        if(this.highestPrice== null){
            this.highestPrice=new AtomicInteger(this.lastPrice);
        }else {
            this.highestPrice.set(this.lastPrice);
        }
        if(this.lowestPrice== null){
            this.lowestPrice=new AtomicInteger(this.lastPrice);
        }else {
            this.lowestPrice.set(this.lastPrice);
        }
        if(this.nowPrice== null){
            this.nowPrice=new AtomicInteger(this.lastPrice);
        }else {
            this.nowPrice.set(this.lastPrice);
        }
        this.maxPrice= (int) (this.lastPrice*1.1);
        this.minPrice= (int) (this.lastPrice*0.9);
        stockHistoryEntities = new ArrayList<>();
        stockDetailEntity=new StockDetailEntity(this.stockCode);
        startTime=LocalDateTime.now();
        endTime= startTime.plusSeconds(50);
    }


    public void updatePrice(Integer price){
        nowPrice.set(price);
        highestPrice.set(Math.max(highestPrice.get(),price));
        lowestPrice.set(Math.min(lowestPrice.get(),price));
    }

    @Override
    public void dailyClear() {
        this.lastPrice=nowPrice.get();
        this.stockHistoryEntities.add(StockEntity.builder()
                .lastPrice(this.lastPrice)
                .nowPrice(this.nowPrice)
                .highestPrice(this.highestPrice)
                .lowestPrice(this.lowestPrice)
                .build());
        refresh();
    }
}
