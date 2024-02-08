package com.xing.RealStock2.context;

import com.xing.RealStock2.common.DailyClear;
import com.xing.RealStock2.entity.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;


public class StockContext {
    @Getter
    @Setter
    private volatile static ExchangeStateEnum exchangeStateEnum;
    @Getter
    private static final Map<String,StockEntity> stockEntityMap=new ConcurrentHashMap<>();
    @Getter
    private static final ConcurrentHashMap<String, AccountEntity> userIdMap=new ConcurrentHashMap<>();

    public static TradeTopNEntity fetchTradeTopN(String stockCode, Integer n){
        StockDetailEntity stockDetailEntity = stockEntityMap.get(stockCode).getStockDetailEntity();
        return TradeTopNEntity.builder()
                .buyMap(fetchBuyMap(n,stockDetailEntity))
                .sellMap(fetchSellMap(n,stockDetailEntity))
                .build();
    }

    private static Map<Integer,Long> fetchBuyMap(Integer n, StockDetailEntity stockDetailEntity) {
        Map<Integer,Long> buyMap=new HashMap<>();
        ConcurrentSkipListMap<Integer, AtomicLong> buyPriceMap = stockDetailEntity.getBuyPriceMap();
        NavigableMap<Integer, AtomicLong> descendingMap = buyPriceMap.descendingMap();
        int count = 0;
        for (Map.Entry<Integer, AtomicLong> entry : descendingMap.entrySet()) {
            if (count >= n) {
                break;
            }
            buyMap.put(entry.getKey(),entry.getValue().get());
            count++;
        }
        return buyMap;
    }
    private static Map<Integer,Long> fetchSellMap(Integer n, StockDetailEntity stockDetailEntity) {
        Map<Integer,Long> sellMap=new HashMap<>();
        ConcurrentSkipListMap<Integer, AtomicLong> sellPriceMap = stockDetailEntity.getSellPriceMap();
        int count = 0;
        for (Map.Entry<Integer, AtomicLong> entry : sellPriceMap.entrySet()) {
            if (count >= n) {
                break;
            }
            sellMap.put(entry.getKey(),entry.getValue().get());
            count++;
        }
        return sellMap;
    }

    public static TransactionListEntity fetchTradeMsg(String stockCode, Integer n){
        List<TradeMsg> tradeMessages = stockEntityMap.get(stockCode).getStockDetailEntity().getTradeOnDayMessages();
        int size = tradeMessages.size();
        if(ExchangeStateEnum.isOpen(StockContext.getExchangeStateEnum())){
            if(tradeMessages.size()>n){
                return new TransactionListEntity(tradeMessages.subList(n, size),size,true);
            }else{
                return new TransactionListEntity(new ArrayList<>(),size,true);
            }
        }else {
            return new TransactionListEntity(new ArrayList<>(), 0,StockContext.getExchangeStateEnum()!=ExchangeStateEnum.CLOSE);
        }

    }

    public static void dailyClear(){
        StockContext.setExchangeStateEnum(ExchangeStateEnum.PAUSE);
        stockEntityMap.values().forEach(DailyClear::dailyClear);
        userIdMap.values().forEach(DailyClear::dailyClear);
    }
}
