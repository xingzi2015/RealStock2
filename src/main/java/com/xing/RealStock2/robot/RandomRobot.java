package com.xing.RealStock2.robot;

import com.xing.RealStock2.context.StockContext;
import com.xing.RealStock2.entity.ExchangeStateEnum;
import com.xing.RealStock2.entity.StockEntity;
import com.xing.RealStock2.entity.TradeEntity;
import com.xing.RealStock2.entity.TradeSideEnum;
import com.xing.RealStock2.notify.TradeSuccessNotify;
import com.xing.RealStock2.service.TradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RandomRobot implements Robot{
    @Autowired
    private TradeService tradeService;

    @Autowired
    private TradeSuccessNotify tradeSuccessNotify;

    public void operate(String userId){
        if(ExchangeStateEnum.isOpen(StockContext.getExchangeStateEnum())){
            for(StockEntity stockEntity:StockContext.getStockEntityMap().values()){
                TradeEntity tradeEntity = TradeEntity.builder()
                        .stockCode(stockEntity.getStockCode())
                        .userId(userId)
                        .price(RandomGenerator.generateRandomPrice(stockEntity.getNowPrice().get()))
                        .amount(RandomGenerator.generateRandomVolume(1000))
                        .tradeNotify(tradeSuccessNotify)
                        .build();
                if(TradeSideEnum.getRandomTradeSide()==TradeSideEnum.BUY){
                    tradeService.buy(tradeEntity);
                }else {
                    tradeService.sell(tradeEntity);
                }

            }
        }
    }

}
