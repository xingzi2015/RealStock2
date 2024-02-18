package com.xing.RealStock2.engine;

import com.xing.RealStock2.common.DailyClear;
import com.xing.RealStock2.entity.*;
import com.xing.RealStock2.filter.BuyFilter;
import com.xing.RealStock2.filter.SellFilter;
import com.xing.RealStock2.filter.stock.StockFilter;
import com.xing.RealStock2.notify.GlobalNotify;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class CallAuctionEngine implements AuctionEngine, DailyClear {
    @Autowired
    private List<BuyFilter> buyFilters;

    @Autowired
    private List<SellFilter> sellFilters;
    @Autowired
    private List<StockFilter> stockFilters;
    @Autowired
    private List<GlobalNotify> globalNotifies;

    @Autowired
    private ContinuousAuctionEngine continuousAuctionEngine;

    //匹配价格
    private final Map<String,AtomicInteger> matchingPriceMap= new ConcurrentHashMap<>();
    //匹配数量统计
    private final Map<String,Long> matchingBuyAmountMap = new ConcurrentHashMap<>();
    private final Map<String,ConcurrentSkipListMap<Integer,Long>> stockBuyPriceMap= new ConcurrentHashMap<>();
    private final Map<String,Long> matchingSellAmountMap = new ConcurrentHashMap<>();
    private final Map<String,ConcurrentSkipListMap<Integer,Long>> stockSellPriceMap= new ConcurrentHashMap<>();
    //详情
    private final Map<String, ConcurrentSkipListMap<Integer, List<TradeEntity>>> buyTradeEntitiesMap= new ConcurrentHashMap<>();
    private final Map<String,ConcurrentSkipListMap<Integer,List<TradeEntity>>> sellTradeEntitiesMap=new ConcurrentHashMap<>();


    @Override
    public synchronized ResultEntity buy(TradeEntity buyTradeEntity) {
        Optional<ResultEntity> resultEntity= stockFilters.stream().map(entity-> entity.doFilter(buyTradeEntity)).filter(rst->!rst.getSuccess()).findFirst();
        if(resultEntity.isPresent()){
            return resultEntity.get();
        }
        resultEntity= buyFilters.stream().map(entity-> entity.doFilter(buyTradeEntity)).filter(rst->!rst.getSuccess()).findFirst();
        if(resultEntity.isPresent()){
            return resultEntity.get();
        }

        Integer buyPrice = buyTradeEntity.getPrice();
        String stockCode = buyTradeEntity.getStockCode();
        matchingPriceMap.putIfAbsent(stockCode,new AtomicInteger(buyPrice));

        buyTradeEntitiesMap.putIfAbsent(stockCode,new ConcurrentSkipListMap<>());
        buyTradeEntitiesMap.get(stockCode).putIfAbsent(buyPrice, Collections.synchronizedList(new ArrayList<>()));
        buyTradeEntitiesMap.get(stockCode).get(buyPrice).add(buyTradeEntity);

        stockBuyPriceMap.putIfAbsent(stockCode,new ConcurrentSkipListMap<>());
        Map<Integer, Long> priceMap = stockBuyPriceMap.get(stockCode);
        priceMap.putIfAbsent(buyPrice,0L);
        priceMap.put(buyPrice,priceMap.get(buyPrice)+buyTradeEntity.getAmount());

        buyTradeEntity.getTradeNotify().notifyWaitingMatch(buyTradeEntity);
        if(tryMatching(stockCode, buyPrice,TradeSideEnum.BUY)){
            globalNotifies.forEach(n->n.callNotifyWaitingMatch(
                    stockCode,matchingPriceMap.get(stockCode).get(), matchingBuyAmountMap.get(stockCode), matchingBuyAmountMap.get(stockCode))
            );
        }
        return new ResultEntity(true,buyTradeEntity);
    }

    @Override
    public synchronized ResultEntity sell(TradeEntity sellTradeEntity) {
        //过滤器
        Optional<ResultEntity> resultEntity= stockFilters.stream().map(entity-> entity.doFilter(sellTradeEntity)).filter(rst->!rst.getSuccess()).findFirst();
        if(resultEntity.isPresent()){
            return resultEntity.get();
        }
        resultEntity= sellFilters.stream().map(entity-> entity.doFilter(sellTradeEntity)).filter(rst->!rst.getSuccess()).findFirst();
        if(resultEntity.isPresent()){
            return resultEntity.get();
        }

        Integer sellPrice = sellTradeEntity.getPrice();
        String stockCode = sellTradeEntity.getStockCode();
        matchingPriceMap.putIfAbsent(stockCode,new AtomicInteger(sellPrice));

        sellTradeEntitiesMap.putIfAbsent(stockCode,new ConcurrentSkipListMap<>());
        sellTradeEntitiesMap.get(stockCode).putIfAbsent(sellPrice, Collections.synchronizedList(new ArrayList<>()));
        sellTradeEntitiesMap.get(stockCode).get(sellPrice).add(sellTradeEntity);

        stockSellPriceMap.putIfAbsent(stockCode,new ConcurrentSkipListMap<>());
        Map<Integer, Long> priceMap = stockSellPriceMap.get(stockCode);
        priceMap.putIfAbsent(sellPrice,0L);
        priceMap.put(sellPrice,priceMap.get(sellPrice)+sellTradeEntity.getAmount());


        sellTradeEntity.getTradeNotify().notifyWaitingMatch(sellTradeEntity);
        if(tryMatching(stockCode, sellPrice,TradeSideEnum.SELL)){
            globalNotifies.forEach(n->n.callNotifyWaitingMatch(
                    stockCode,matchingPriceMap.get(stockCode).get(), matchingBuyAmountMap.get(stockCode), matchingSellAmountMap.get(stockCode))
            );
        }

        return new ResultEntity(true,sellTradeEntity);
    }

    public synchronized ResultEntity cancel(TradeEntity tradeEntity){
        while (tradeEntity.getAmount()>0){
            long amount = tradeEntity.getAmount();
            if(tradeEntity.minusAmount(amount)){
                TradeMsg tradeMsg = TradeMsg.builder().stockCode(tradeEntity.getStockCode()).intendPrice(tradeEntity.getPrice()).amount(amount).dateTime(LocalDateTime.now()).build();
                tradeEntity.getTradeNotify().notifyCancelInfo(tradeEntity.getTradeSide(),tradeEntity.getUserId(),tradeMsg);
                return new ResultEntity(true);
            }
        }
        return new ResultEntity(false,"该订单已经成功交易");
    }

    private boolean tryMatching(String stockCode,Integer intentPrice,TradeSideEnum tradeSideEnum){

        int lastPrice=matchingPriceMap.get(stockCode).get();
        Integer nextPrice=lastPrice;
        ConcurrentSkipListMap<Integer,Long> buyPriceMap=stockBuyPriceMap.get(stockCode);
        ConcurrentSkipListMap<Integer,Long> sellPriceMap=stockSellPriceMap.get(stockCode);
        if(buyPriceMap==null || sellPriceMap==null){
            return false;
        }
        long lastBuyAmount= matchingBuyAmountMap.getOrDefault(stockCode,0L);
        long lastSellAmount= matchingSellAmountMap.getOrDefault(stockCode,0L);

        while (true){

           long buyAmount=0;
           for(Map.Entry<Integer,Long> entry: buyPriceMap.descendingMap().entrySet()){
               if(entry.getKey()>=nextPrice){
                   buyAmount+=entry.getValue();
               }
           }
            long sellAmount=0;
            for(Map.Entry<Integer,Long> entry: sellPriceMap.entrySet()){
                if(entry.getKey()<=nextPrice){
                    sellAmount+=entry.getValue();
                }
            }
            if(Math.min(lastBuyAmount,lastSellAmount)>= Math.min(buyAmount,sellAmount) && nextPrice!=lastPrice){
                break;
            }
            lastBuyAmount=buyAmount;
            lastSellAmount=sellAmount;
            lastPrice=nextPrice;

            if(tradeSideEnum==TradeSideEnum.BUY){
                nextPrice=sellPriceMap.higherKey(lastPrice);
            }else{
                nextPrice=buyPriceMap.lowerKey(lastPrice);
            }
            if(nextPrice == null){
                break;
            }
       }
        matchingPriceMap.get(stockCode).set(lastPrice);
        matchingBuyAmountMap.put(stockCode, lastBuyAmount);
        matchingSellAmountMap.put(stockCode, lastSellAmount);
        int finalLastPrice = lastPrice;
        globalNotifies.forEach(t->t.notifyPreMatching(stockCode, finalLastPrice));
        return Math.min(lastBuyAmount,lastSellAmount)>0;
    }

    public synchronized void matching(){
        for(Map.Entry<String,AtomicInteger> entry:matchingPriceMap.entrySet()){
            String stockCode=entry.getKey();
            Integer price =entry.getValue().get();
            long amount = Math.min(matchingBuyAmountMap.getOrDefault(stockCode,0L), matchingSellAmountMap.getOrDefault(stockCode,0L));
            if(amount>0){
                ConcurrentSkipListMap<Integer, List<TradeEntity>> buyMap=buyTradeEntitiesMap.get(stockCode);
                ConcurrentSkipListMap<Integer, List<TradeEntity>> sellMap=sellTradeEntitiesMap.get(stockCode);
                doMatching(amount,buyMap.descendingMap(),price);
                doMatching(amount,sellMap,price);
            }
        }

    }

    private void doMatching(Long amount, Map<Integer, List<TradeEntity>> map, Integer price){
        long restAmount=amount;
        for (Map.Entry<Integer, List<TradeEntity>> sellEntry : map.entrySet()) {
            for (TradeEntity tradeEntity : sellEntry.getValue()) {
                if(restAmount>0){
                    long tradeAmount = Math.min(restAmount,tradeEntity.getAmount());
                    tradeEntity.setAmount(tradeEntity.getAmount()-tradeAmount);
                    restAmount-=tradeAmount;

                    TradeMsg tradeMsg = TradeMsg.builder()
                            .stockCode(tradeEntity.getStockCode()).intendPrice(tradeEntity.getPrice()).transactionPrice(price)
                            .uuid(tradeEntity.getUuid()).amount(tradeAmount).dateTime(LocalDateTime.now()).build();

                    tradeEntity.getTradeNotify().notifyTradeInfo(tradeEntity.getTradeSide(),tradeEntity.getUserId(),tradeMsg);
                    globalNotifies.forEach(t->t.notifyMatching(tradeEntity.getTradeSide(),tradeMsg));
                }else {
                    break;
                }
            }
        }
    }

    @Override
    public void dailyClear() {
        for (Map.Entry<String, ConcurrentSkipListMap<Integer, List<TradeEntity>>> entry : buyTradeEntitiesMap.entrySet()) {
            String stockCode = entry.getKey();
            ConcurrentSkipListMap<Integer, List<TradeEntity>> priceMap = entry.getValue();
            for (Map.Entry<Integer, List<TradeEntity>> priceEntry : priceMap.entrySet()) {
                Integer price = priceEntry.getKey();
                List<TradeEntity> tradeEntities = priceEntry.getValue();
                for (TradeEntity tradeEntity : tradeEntities) {
                    if(tradeEntity.getAmount()>0){
                        cancel(tradeEntity);
                        log.info("统一取消集合竞价买，代码：{}，价格：{}, 订单号：{}",stockCode,price,tradeEntity.getUuid());
                    }

                }
            }
        }
        buyTradeEntitiesMap.clear();
        for (Map.Entry<String, ConcurrentSkipListMap<Integer, List<TradeEntity>>> entry : sellTradeEntitiesMap.entrySet()) {
            String stockCode = entry.getKey();
            ConcurrentSkipListMap<Integer, List<TradeEntity>> priceMap = entry.getValue();
            for (Map.Entry<Integer, List<TradeEntity>> priceEntry : priceMap.entrySet()) {
                Integer price = priceEntry.getKey();
                List<TradeEntity> tradeEntities = priceEntry.getValue();
                for (TradeEntity tradeEntity : tradeEntities) {
                    if(tradeEntity.getAmount()>0) {
                        cancel(tradeEntity);
                        log.info("统一取消集合竞价卖，代码：{}，价格：{}, 订单号：{}", stockCode, price, tradeEntity.getUuid());
                    }
                }
            }
        }
        sellTradeEntitiesMap.clear();
        matchingPriceMap.clear();
        matchingBuyAmountMap.clear();
        matchingSellAmountMap.clear();
        stockBuyPriceMap.clear();
        stockSellPriceMap.clear();
    }
    public void moveToContinous(){
        for (Map.Entry<String, ConcurrentSkipListMap<Integer, List<TradeEntity>>> entry : buyTradeEntitiesMap.entrySet()) {
            String stockCode = entry.getKey();
            ConcurrentSkipListMap<Integer, List<TradeEntity>> priceMap = entry.getValue();
            for (Map.Entry<Integer, List<TradeEntity>> priceEntry : priceMap.entrySet()) {
                Integer price = priceEntry.getKey();
                List<TradeEntity> tradeEntities = priceEntry.getValue();
                for (TradeEntity tradeEntity : tradeEntities) {
                    if(tradeEntity.getAmount()>0){
                        continuousAuctionEngine.move(tradeEntity);
                        log.info("移动到连续竞价买，代码：{}，价格：{}, 订单号：{}",stockCode,price,tradeEntity.getUuid());
                    }
                }
            }
        }
        buyTradeEntitiesMap.clear();
        for (Map.Entry<String, ConcurrentSkipListMap<Integer, List<TradeEntity>>> entry : sellTradeEntitiesMap.entrySet()) {
            String stockCode = entry.getKey();
            ConcurrentSkipListMap<Integer, List<TradeEntity>> priceMap = entry.getValue();
            for (Map.Entry<Integer, List<TradeEntity>> priceEntry : priceMap.entrySet()) {
                Integer price = priceEntry.getKey();
                List<TradeEntity> tradeEntities = priceEntry.getValue();
                for (TradeEntity tradeEntity : tradeEntities) {
                    if(tradeEntity.getAmount()>0){
                        continuousAuctionEngine.move(tradeEntity);
                        log.info("移动到连续竞价卖，代码：{}，价格：{}, 订单号：{}",stockCode,price,tradeEntity.getUuid());
                    }

                }
            }
        }
        sellTradeEntitiesMap.clear();
    }
}
