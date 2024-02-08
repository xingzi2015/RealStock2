package com.xing.RealStock2.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.xing.RealStock2.common.DailyClear;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Data
@Builder
public class StockEntity implements DailyClear {

    private String stockCode;

    private AtomicInteger nowPrice;

    private Integer lastPrice;

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
        stockHistoryEntities = new ArrayList<>();
        stockDetailEntity=new StockDetailEntity(this.stockCode);
    }


    public void updatePrice(Integer price){
        nowPrice.set(price);
        highestPrice.set(Math.max(highestPrice.get(),price));
        lowestPrice.set(Math.min(lowestPrice.get(),price));
    }

    @Override
    public void dailyClear() {
        this.stockHistoryEntities.add(StockEntity.builder()
                .lastPrice(this.lastPrice)
                .nowPrice(this.nowPrice)
                .highestPrice(this.highestPrice)
                .lowestPrice(this.lowestPrice)
                .build());
        refresh();
    }
}
