package com.xing.RealStock2.notify;

import com.xing.RealStock2.context.StockContext;
import com.xing.RealStock2.entity.AccountEntity;
import com.xing.RealStock2.entity.TradeEntity;
import com.xing.RealStock2.entity.TradeMsg;
import com.xing.RealStock2.entity.TradeSideEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TradeSuccessNotify implements TradeNotify{
    @Override
    public void notifyTradeInfo(TradeSideEnum tradeSide, String userId, TradeMsg tradeMsg) {
        AccountEntity accountEntity= StockContext.getUserIdMap().get(userId);
        switch (tradeSide){
            case BUY -> {
                accountEntity.moneyToStock(tradeMsg);
            }
            case SELL -> {
                accountEntity.stockToMoney(tradeMsg);
            }
        }
        log.info("成交{}，用户：{}，交易信息：{}",tradeSide,userId,tradeMsg);
    }

    @Override
    public void notifyWaitingMatch(TradeEntity tradeEntity) {
        AccountEntity accountEntity= StockContext.getUserIdMap().get(tradeEntity.getUserId());
        accountEntity.getMatchingTradeEntityMap().put(tradeEntity.getUuid(),tradeEntity);
        log.info("等待成交中，交易信息：{}",tradeEntity);
    }

    @Override
    public void notifyCancelInfo(TradeSideEnum tradeSide, String userId, TradeMsg tradeMsg) {
        AccountEntity accountEntity= StockContext.getUserIdMap().get(userId);
        switch (tradeSide){
            case BUY -> {
                accountEntity.unfreezeMoney(tradeMsg.getIntendPrice(),tradeMsg.getAmount());
            }
            case SELL -> {
                accountEntity.unfreezeStock(tradeMsg.getStockCode(),tradeMsg.getAmount());
            }
        }
        log.info("交易撤销成功{}，用户：{}，交易信息：{}",tradeSide,userId,tradeMsg);
    }
}
