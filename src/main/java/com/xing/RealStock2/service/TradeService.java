package com.xing.RealStock2.service;

import com.xing.RealStock2.context.StockContext;
import com.xing.RealStock2.engine.CallAuctionEngine;
import com.xing.RealStock2.engine.ContinuousAuctionEngine;
import com.xing.RealStock2.entity.ExchangeStateEnum;
import com.xing.RealStock2.entity.ResultEntity;
import com.xing.RealStock2.entity.TradeEntity;
import com.xing.RealStock2.notify.TradeSuccessNotify;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TradeService {
    @Autowired
    private CallAuctionEngine callAuctionEngine;
    @Autowired
    private ContinuousAuctionEngine continuousAuctionEngine;
    @Autowired
    private TradeSuccessNotify tradeSuccessNotify;

    public ResultEntity buy(TradeEntity tradeEntity){
        tradeEntity.setTradeNotify(tradeSuccessNotify);
        if(StockContext.getExchangeStateEnum()== ExchangeStateEnum.CALL_AUCTION){
           return callAuctionEngine.buy(tradeEntity);
        }else if (StockContext.getExchangeStateEnum()== ExchangeStateEnum.CONTINUOUS_AUCTION){
           return continuousAuctionEngine.buy(tradeEntity);
        }
        return new ResultEntity(false,"交易暂停");
    }

    public ResultEntity sell(TradeEntity tradeEntity){
        tradeEntity.setTradeNotify(tradeSuccessNotify);
        if(StockContext.getExchangeStateEnum()== ExchangeStateEnum.CALL_AUCTION){
            return callAuctionEngine.sell(tradeEntity);
        }else if (StockContext.getExchangeStateEnum()== ExchangeStateEnum.CONTINUOUS_AUCTION){
            return continuousAuctionEngine.sell(tradeEntity);
        }
        return new ResultEntity(false,"交易暂停");
    }

    public ResultEntity cancel(String uuid,String userId){
        if(StockContext.getExchangeStateEnum()== ExchangeStateEnum.CALL_AUCTION){
            return new ResultEntity(false,"集合竞价无法撤销订单");
        }else if (StockContext.getExchangeStateEnum()== ExchangeStateEnum.CONTINUOUS_AUCTION){
            TradeEntity tradeEntity = StockContext.getUserIdMap().get(userId).getMatchingTradeEntityMap().get(uuid);
            if(tradeEntity==null){
                return new ResultEntity(false,"没有当前订单");
            }
            tradeEntity.setTradeNotify(tradeSuccessNotify);
            continuousAuctionEngine.cancel(tradeEntity);
        }
        return new ResultEntity(false,"交易暂停");
    }
}
