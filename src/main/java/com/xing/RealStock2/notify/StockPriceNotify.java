package com.xing.RealStock2.notify;

import com.xing.RealStock2.context.StockContext;
import com.xing.RealStock2.entity.StockEntity;
import com.xing.RealStock2.entity.TradeMsg;
import com.xing.RealStock2.entity.TradeSideEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class StockPriceNotify implements GlobalNotify{
    @Override
    public void notifyMatching(TradeSideEnum tradeSide, TradeMsg tradeMsg) {
        StockEntity stockEntity = StockContext.getStockEntityMap().get(tradeMsg.getStockCode());
        //设置价格
        stockEntity.updatePrice(tradeMsg.getTransactionPrice());
        stockEntity.getStockDetailEntity().getTradeOnDayMessages().add(tradeMsg);

        updatePriceMap(tradeSide, tradeMsg.getTransactionPrice(),tradeMsg.getAmount(), stockEntity);
    }


    @Override
    public void notifyPreMatching(String stockCode, Integer price) {
        StockEntity stockEntity = StockContext.getStockEntityMap().get(stockCode);
        stockEntity.updatePrice(price);
        stockEntity.getStockDetailEntity().getTradeOnDayMessages().add(TradeMsg.builder().stockCode(stockCode).transactionPrice(price).amount(0L).dateTime(LocalDateTime.now()).build());
    }

    @Override
    public void continousNotifyWaitingMatch(TradeSideEnum tradeSide, TradeMsg tradeMsg) {
        StockEntity stockEntity = StockContext.getStockEntityMap().get(tradeMsg.getStockCode());
        if(tradeSide==TradeSideEnum.BUY){
            stockEntity.getStockDetailEntity().getBuyPriceMap().putIfAbsent(tradeMsg.getIntendPrice(),new AtomicLong(0));
            stockEntity.getStockDetailEntity().getBuyPriceMap().get(tradeMsg.getIntendPrice()).addAndGet(tradeMsg.getAmount());
        }else if(tradeSide==TradeSideEnum.SELL){
            stockEntity.getStockDetailEntity().getSellPriceMap().putIfAbsent(tradeMsg.getIntendPrice(),new AtomicLong(0));
            stockEntity.getStockDetailEntity().getSellPriceMap().get(tradeMsg.getIntendPrice()).addAndGet(tradeMsg.getAmount());
        }else{
            log.error("异常场景, tradeSide="+tradeSide.name());
        }
    }

    @Override
    public void callNotifyWaitingMatch( String stockCode,Integer price,Long buyAmount,Long sellAmount) {
        StockEntity stockEntity = StockContext.getStockEntityMap().get(stockCode);
        stockEntity.getStockDetailEntity().getBuyPriceMap().clear();
        stockEntity.getStockDetailEntity().getBuyPriceMap().put(price,new AtomicLong(buyAmount));
        stockEntity.getStockDetailEntity().getSellPriceMap().clear();
        stockEntity.getStockDetailEntity().getSellPriceMap().put(price,new AtomicLong(sellAmount));
    }


    @Override
    public void notifyCancel(TradeSideEnum tradeSide, TradeMsg tradeMsg) {
        StockEntity stockEntity = StockContext.getStockEntityMap().get(tradeMsg.getStockCode());
        updatePriceMap(tradeSide, tradeMsg.getIntendPrice(),tradeMsg.getAmount(), stockEntity);
    }

    private static void updatePriceMap(TradeSideEnum tradeSide, Integer price,Long amount, StockEntity stockEntity) {
        try{
            if(tradeSide ==TradeSideEnum.BUY){
                stockEntity.getStockDetailEntity().getBuyPriceMap().putIfAbsent(price,new AtomicLong(0));
                stockEntity.getStockDetailEntity().getBuyPriceMap().get(price).addAndGet(-1* amount);
            }else if(tradeSide ==TradeSideEnum.SELL){
                stockEntity.getStockDetailEntity().getSellPriceMap().putIfAbsent(price,new AtomicLong(0));
                stockEntity.getStockDetailEntity().getSellPriceMap().get(price).addAndGet(-1* amount);
            }else{
                log.error("异常场景, tradeSide="+ tradeSide.name());
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
