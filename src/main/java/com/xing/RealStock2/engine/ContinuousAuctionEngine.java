package com.xing.RealStock2.engine;

import com.xing.RealStock2.common.DailyClear;
import com.xing.RealStock2.entity.ResultEntity;
import com.xing.RealStock2.entity.TradeEntity;
import com.xing.RealStock2.entity.TradeMsg;
import com.xing.RealStock2.entity.TradeSideEnum;
import com.xing.RealStock2.filter.BuyFilter;
import com.xing.RealStock2.filter.SellFilter;
import com.xing.RealStock2.filter.stock.ExchangeFilter;
import com.xing.RealStock2.filter.stock.StockFilter;
import com.xing.RealStock2.notify.GlobalNotify;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ContinuousAuctionEngine implements AuctionEngine, DailyClear {
    private final Map<String,PriorityQueue<TradeEntity>> buyTradeEntitiesMap= new ConcurrentHashMap<>();

    private final Map<String,PriorityQueue<TradeEntity>> sellTradeEntitiesMap=new ConcurrentHashMap<>();
    @Autowired
    private List<BuyFilter> buyFilters;

    @Autowired
    private List<SellFilter> sellFilters;
    @Autowired
    private List<StockFilter> stockFilters;
    @Autowired
    private List<GlobalNotify> globalNotifies;

    @Autowired
    private ExchangeFilter exchangeFilter;

    private TradeEntity peek(PriorityQueue<TradeEntity> entityPriorityQueue){
        while (true){
            TradeEntity peek = entityPriorityQueue.peek();
            if(peek ==null){
                return null;
            } else if (peek.getAmount()>0){
                return peek;
            }else {
                entityPriorityQueue.poll();
            }
        }
    }

    @Override
    public synchronized ResultEntity buy(TradeEntity buyTradeEntity){
        //过滤器
        Optional<ResultEntity> resultEntity= stockFilters.stream().map(entity-> entity.doFilter(buyTradeEntity)).filter(rst->!rst.getSuccess()).findFirst();
        if(resultEntity.isPresent()){
            return resultEntity.get();
        }
        resultEntity= buyFilters.stream().map(entity-> entity.doFilter(buyTradeEntity)).filter(rst->!rst.getSuccess()).findFirst();
        if(resultEntity.isPresent()){
            return resultEntity.get();
        }

        PriorityQueue<TradeEntity> sellTradeEntities=sellTradeEntitiesMap.get(buyTradeEntity.getStockCode());
        while (!CollectionUtils.isEmpty(sellTradeEntities)){
            TradeEntity peekTradeEntity = peek(sellTradeEntities);
            if (peekTradeEntity==null) break;
            long tradeAmount = match(buyTradeEntity, peekTradeEntity);
            if (tradeAmount<=0) break;
            if( buyTradeEntity.minusAmount(tradeAmount) && peekTradeEntity.minusAmount(tradeAmount)){
                TradeEntity sellTradeEntity= sellTradeEntities.peek();
                assert sellTradeEntity != null;
                LocalDateTime now = LocalDateTime.now();
                sellTradeEntity.getTradeNotify().notifyTradeInfo(TradeSideEnum.SELL,sellTradeEntity.getUserId(),TradeMsg.builder().uuid(sellTradeEntity.getUuid()).stockCode(buyTradeEntity.getStockCode()).intendPrice(sellTradeEntity.getPrice()).transactionPrice(sellTradeEntity.getPrice()).amount(tradeAmount).dateTime(now).build());
                buyTradeEntity.getTradeNotify().notifyTradeInfo(TradeSideEnum.BUY, buyTradeEntity.getUserId(), TradeMsg.builder().uuid(buyTradeEntity.getUuid()).stockCode(buyTradeEntity.getStockCode()).intendPrice(buyTradeEntity.getPrice()).transactionPrice(sellTradeEntity.getPrice()).amount(tradeAmount).dateTime(now).build());
                globalNotifies.forEach(t->t.notifyMatching(TradeSideEnum.SELL,TradeMsg.builder().stockCode(buyTradeEntity.getStockCode()).transactionPrice(sellTradeEntity.getPrice()).amount(tradeAmount).dateTime(now).build()));
            }

        }
        if(buyTradeEntity.getAmount()>0){
            buyTradeEntitiesMap.putIfAbsent(buyTradeEntity.getStockCode(),generateBuyQueue());
            buyTradeEntitiesMap.get(buyTradeEntity.getStockCode()).add(buyTradeEntity);
            TradeMsg tradeMsg = TradeMsg.builder().stockCode(buyTradeEntity.getStockCode()).intendPrice(buyTradeEntity.getPrice()).amount(buyTradeEntity.getAmount()).dateTime(LocalDateTime.now()).build();
            globalNotifies.forEach(t->t.continousNotifyWaitingMatch(TradeSideEnum.BUY,tradeMsg));
            buyTradeEntity.getTradeNotify().notifyWaitingMatch(buyTradeEntity);
            return new ResultEntity(true,buyTradeEntity);
        }

        return new ResultEntity(true);
    }

    private PriorityQueue<TradeEntity> generateBuyQueue(){
        return new PriorityQueue<>(Comparator.comparingInt(TradeEntity::getPrice).reversed());
    }
    private PriorityQueue<TradeEntity> generateSellQueue(){
        return new PriorityQueue<>();
    }

    @Override
    public synchronized ResultEntity sell(TradeEntity sellTradeEntity){
        //过滤器
        Optional<ResultEntity> resultEntity= stockFilters.stream().map(entity-> entity.doFilter(sellTradeEntity)).filter(rst->!rst.getSuccess()).findFirst();
        if(resultEntity.isPresent()){
            return resultEntity.get();
        }
        resultEntity= sellFilters.stream().map(entity-> entity.doFilter(sellTradeEntity)).filter(rst->!rst.getSuccess()).findFirst();
        if(resultEntity.isPresent()){
            return resultEntity.get();
        }

        PriorityQueue<TradeEntity> buyTradeEntities=buyTradeEntitiesMap.get(sellTradeEntity.getStockCode());
        while (!CollectionUtils.isEmpty(buyTradeEntities)){
            TradeEntity peekTradeEntity = peek(buyTradeEntities);
            if (peekTradeEntity==null) break;
            long tradeAmount = match(peekTradeEntity, sellTradeEntity);
            if (tradeAmount<=0) break;
            if(peekTradeEntity.minusAmount(tradeAmount) && sellTradeEntity.minusAmount(tradeAmount)){
                TradeEntity buyTradeEntity= buyTradeEntities.peek();
                assert buyTradeEntity != null;
                buyTradeEntity.getTradeNotify().notifyTradeInfo(TradeSideEnum.BUY,buyTradeEntity.getUserId(),TradeMsg.builder().uuid(buyTradeEntity.getUuid()).stockCode(sellTradeEntity.getStockCode()).intendPrice(buyTradeEntity.getPrice()).transactionPrice(buyTradeEntity.getPrice()).amount(tradeAmount).dateTime(LocalDateTime.now()).build());
                sellTradeEntity.getTradeNotify().notifyTradeInfo(TradeSideEnum.SELL,sellTradeEntity.getUserId(),TradeMsg.builder().uuid(sellTradeEntity.getUuid()).stockCode(sellTradeEntity.getStockCode()).intendPrice(sellTradeEntity.getPrice()).transactionPrice(buyTradeEntity.getPrice()).amount(tradeAmount).dateTime(LocalDateTime.now()).build());
                globalNotifies.forEach(t->t.notifyMatching(TradeSideEnum.BUY,TradeMsg.builder().stockCode(sellTradeEntity.getStockCode()).transactionPrice(buyTradeEntity.getPrice()).amount(tradeAmount).dateTime(LocalDateTime.now()).build()));
            }
        }
        if(sellTradeEntity.getAmount()>0){
            sellTradeEntitiesMap.putIfAbsent(sellTradeEntity.getStockCode(),generateSellQueue());
            sellTradeEntitiesMap.get(sellTradeEntity.getStockCode()).add(sellTradeEntity);
            TradeMsg tradeMsg = TradeMsg.builder().stockCode(sellTradeEntity.getStockCode()).intendPrice(sellTradeEntity.getPrice()).amount(sellTradeEntity.getAmount()).dateTime(LocalDateTime.now()).build();
            globalNotifies.forEach(t->t.continousNotifyWaitingMatch(TradeSideEnum.SELL,tradeMsg));
            sellTradeEntity.getTradeNotify().notifyWaitingMatch(sellTradeEntity);
            return new ResultEntity(true,sellTradeEntity);
        }
        return new ResultEntity(true);
    }

    public synchronized ResultEntity cancel(TradeEntity tradeEntity){
        while (tradeEntity.getAmount()>0){
            long amount = tradeEntity.getAmount();
            if(tradeEntity.minusAmount(amount)){
                TradeMsg tradeMsg = TradeMsg.builder().stockCode(tradeEntity.getStockCode()).intendPrice(tradeEntity.getPrice()).amount(amount).dateTime(LocalDateTime.now()).build();
                tradeEntity.getTradeNotify().notifyCancelInfo(tradeEntity.getTradeSide(),tradeEntity.getUserId(),tradeMsg);
                globalNotifies.forEach(t->t.notifyCancel(tradeEntity.getTradeSide(),tradeMsg));
                return new ResultEntity(true);
            }
        }
        return new ResultEntity(false,"该订单已经成功交易");

    }

    public synchronized ResultEntity move(TradeEntity tradeEntity){
        if(tradeEntity.getTradeSide()==TradeSideEnum.BUY){
            buyTradeEntitiesMap.putIfAbsent(tradeEntity.getStockCode(),generateBuyQueue());
            buyTradeEntitiesMap.get(tradeEntity.getStockCode()).add(tradeEntity);
            TradeMsg tradeMsg = TradeMsg.builder().stockCode(tradeEntity.getStockCode()).intendPrice(tradeEntity.getPrice()).amount(tradeEntity.getAmount()).dateTime(LocalDateTime.now()).build();
            globalNotifies.forEach(t->t.continousNotifyWaitingMatch(TradeSideEnum.BUY,tradeMsg));
        }else {
            sellTradeEntitiesMap.putIfAbsent(tradeEntity.getStockCode(),generateSellQueue());
            sellTradeEntitiesMap.get(tradeEntity.getStockCode()).add(tradeEntity);
            TradeMsg tradeMsg = TradeMsg.builder().stockCode(tradeEntity.getStockCode()).intendPrice(tradeEntity.getPrice()).amount(tradeEntity.getAmount()).dateTime(LocalDateTime.now()).build();
            globalNotifies.forEach(t->t.continousNotifyWaitingMatch(TradeSideEnum.SELL,tradeMsg));
        }
        tradeEntity.getTradeNotify().notifyWaitingMatch(tradeEntity);
        return new ResultEntity(true,tradeEntity);
    }
    private Long match(TradeEntity buyTradeEntity,TradeEntity sellTradeEntity){

        if(buyTradeEntity.getTradeSide()!= TradeSideEnum.BUY || sellTradeEntity.getTradeSide()!= TradeSideEnum.SELL){
            log.error("错误的交易方向，买:{}，卖：{}",buyTradeEntity.getTradeSide(),sellTradeEntity.getTradeSide());
            return 0L;
        }
        if(buyTradeEntity.getPrice().compareTo(sellTradeEntity.getPrice())<0){
            return 0L;
        }
        return Math.min(buyTradeEntity.getAmount(),sellTradeEntity.getAmount());

    }

    @Override
    public void dailyClear() {
        for (Map.Entry<String, PriorityQueue<TradeEntity>> entry : buyTradeEntitiesMap.entrySet()) {
            String stockCode = entry.getKey();
            PriorityQueue<TradeEntity> tradeEntityQueue = entry.getValue();
            for (TradeEntity tradeEntity : tradeEntityQueue) {
                cancel(tradeEntity);
                log.info("统一取消连续报价买，代码：{}， 订单号：{}",stockCode,tradeEntity.getUuid());
            }
        }
        buyTradeEntitiesMap.clear();
        for (Map.Entry<String, PriorityQueue<TradeEntity>> entry : sellTradeEntitiesMap.entrySet()) {
            String stockCode = entry.getKey();
            PriorityQueue<TradeEntity> tradeEntityQueue = entry.getValue();
            for (TradeEntity tradeEntity : tradeEntityQueue) {
                cancel(tradeEntity);
                log.info("统一取消连续报价卖，代码：{}， 订单号：{}",stockCode,tradeEntity.getUuid());
            }
        }
        sellTradeEntitiesMap.clear();
    }
}
