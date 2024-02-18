package com.xing.RealStock2.context;

import com.xing.RealStock2.common.DailyClear;
import com.xing.RealStock2.entity.*;
import lombok.Getter;
import lombok.Setter;

import java.time.*;
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
        Map<Integer,Long> buyMap=new TreeMap<>();
        ConcurrentSkipListMap<Integer, AtomicLong> buyPriceMap = stockDetailEntity.getBuyPriceMap();
        NavigableMap<Integer, AtomicLong> descendingMap = buyPriceMap.descendingMap();
        int count = 0;
        for (Map.Entry<Integer, AtomicLong> entry : descendingMap.entrySet()) {
            if (count >= n) {
                break;
            }
            if(entry.getValue().get()==0){
                continue;
            }
            buyMap.put(entry.getKey(),entry.getValue().get());
            count++;
        }
        return buyMap;
    }
    private static Map<Integer,Long> fetchSellMap(Integer n, StockDetailEntity stockDetailEntity) {
        Map<Integer,Long> sellMap=new TreeMap<>();
        ConcurrentSkipListMap<Integer, AtomicLong> sellPriceMap = stockDetailEntity.getSellPriceMap();
        int count = 0;
        for (Map.Entry<Integer, AtomicLong> entry : sellPriceMap.entrySet()) {
            if (count >= n) {
                break;
            }
            if(entry.getValue().get()==0){
                continue;
            }
            sellMap.put(entry.getKey(),entry.getValue().get());
            count++;
        }
        return sellMap;
    }

    public static TransactionListEntity fetchTradeMsg(String stockCode, Integer n){
        StockEntity stockEntity = getStockEntityMap().get(stockCode);
        List<TradeMsg> tradeMessages = stockEntity.getStockDetailEntity().getTradeOnDayMessages();
        int size = tradeMessages.size();
        if(StockContext.getExchangeStateEnum().isOpen() && tradeMessages.size()>n){

            return TransactionListEntity.builder()
                    .tradeMessages(tradeMessages.subList(n, size))
                    .size(size)
                    .closeFlag(false)
                    .startDate(stockEntity.getStartTime())
                    .endDate(stockEntity.getEndTime())
                    .minPrice(stockEntity.getMinPrice())
                    .maxPrice(stockEntity.getMaxPrice())
                    .build();
        }else {
            return TransactionListEntity.builder()
                    .tradeMessages(new ArrayList<>())
                    .size(size)
                    .closeFlag(StockContext.getExchangeStateEnum().isClose())
                    .build();
        }

    }

//    private static int currentIndex = 0; // 用于记录当前处理的索引位置
//    private static LocalDateTime lastProcessedTime; // 上次处理的时间
//    private static int batchSize = 50; // 计算平均价格的时间间隔（毫秒）
//    private static double lastAveragePrice = 0; // 上一个窗口期的平均价格
//
//    // 处理 TradeMsg 列表，返回包含每个区间的平均价格的 TradeMsg 列表
//    public static List<TradeMsg> processTradeMsgs(List<TradeMsg> tradeMsgs) {
//        List<TradeMsg> result = new ArrayList<>();
//        if (tradeMsgs == null || tradeMsgs.isEmpty()) {
//            return result; // 如果输入为空，则返回空列表
//        }
//
//        while (currentIndex < tradeMsgs.size()) {
//            long totalTransactionPrice = 0;
//            long totalCount = 0;
//            LocalDateTime currentTime = tradeMsgs.get(currentIndex).getDateTime();
//
//            // 遍历交易消息列表，计算每个区间的平均价格
//            while (currentIndex < tradeMsgs.size()) {
//                TradeMsg tradeMsg = tradeMsgs.get(currentIndex);
//                LocalDateTime msgTime = tradeMsg.getDateTime();
//
//                // 如果消息时间超出当前区间，则退出循环
//                if (msgTime.isAfter(currentTime.plus(Duration.ofMillis(batchSize)))) {
//                    break;
//                }
//
//                // 计算总交易金额和总交易量
//                totalTransactionPrice += tradeMsg.getTransactionPrice() * tradeMsg.getAmount();
//                totalCount += tradeMsg.getAmount();
//                currentIndex++;
//            }
//
//            // 计算平均价格
//            double averagePrice = totalCount > 0 ? (double) totalTransactionPrice / totalCount : lastAveragePrice;
//
//            // 创建包含当前区间平均价格的 TradeMsg 对象，并添加到结果列表中
//            TradeMsg tradeMsgWithAverage = TradeMsg.builder().dateTime(currentTime).transactionPrice((int) averagePrice).build();
//
//            result.add(tradeMsgWithAverage);
//
//            // 更新上次处理的时间和上一个窗口期的平均价格
//            lastProcessedTime = currentTime;
//            lastAveragePrice = averagePrice;
//        }
//
//        return result;
//    }

    // 测试函数
    public static void dailyClear(){
        StockContext.setExchangeStateEnum(ExchangeStateEnum.PAUSE);
        stockEntityMap.values().forEach(DailyClear::dailyClear);
        userIdMap.values().forEach(DailyClear::dailyClear);
    }
    public static void pauseClear(){
        stockEntityMap.values().forEach(entity->entity.getStockDetailEntity().pauseClear());
    }
}
