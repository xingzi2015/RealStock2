package com.xing.RealStock2.entity;

import com.xing.RealStock2.common.DailyClear;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;


@Data
@Slf4j
public class StockDetailEntity implements DailyClear {
    private String stockCode;
    //成交信息
    private List<TradeMsg> tradeOnDayMessages;
    //未成交买信息
    private ConcurrentSkipListMap<Integer, AtomicLong> buyPriceMap;
    //未成交卖信息
    private ConcurrentSkipListMap<Integer,AtomicLong> sellPriceMap;

    public StockDetailEntity(String stockCode) {
        this.stockCode=stockCode;
        resfresh();
    }

    public void resfresh(){
        tradeOnDayMessages=Collections.synchronizedList(new ArrayList<>());
        buyPriceMap= new ConcurrentSkipListMap<>();
        sellPriceMap=new ConcurrentSkipListMap<>();
    }

    private void checkPrice(){
        for(Map.Entry<Integer, AtomicLong> entry:buyPriceMap.entrySet()){
            if(entry.getValue().get()>0){
                log.warn("代码{}, 买入价格{}，数量不应该大于0,数量：{}",stockCode,entry.getKey(),entry.getValue());
            }
        }
        for(Map.Entry<Integer, AtomicLong> entry:sellPriceMap.entrySet()){
            if(entry.getValue().get()>0){
                log.warn("代码{}, 卖出价格{}，数量不应该大于0,数量：{}",stockCode,entry.getKey(),entry.getValue());
            }
        }
    }
    @Override
    public void dailyClear() {
        checkPrice();
        resfresh();
    }

    public void pauseClear(){
        buyPriceMap= new ConcurrentSkipListMap<>();
        sellPriceMap=new ConcurrentSkipListMap<>();
    }
}
