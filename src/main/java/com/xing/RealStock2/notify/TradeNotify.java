package com.xing.RealStock2.notify;

import com.xing.RealStock2.entity.TradeEntity;
import com.xing.RealStock2.entity.TradeMsg;
import com.xing.RealStock2.entity.TradeSideEnum;

public interface TradeNotify {
    void notifyTradeInfo(TradeSideEnum tradeSide,String userId, TradeMsg tradeMsg);

    void notifyWaitingMatch(TradeEntity tradeEntity);

    void notifyCancelInfo(TradeSideEnum tradeSide,String userId, TradeMsg tradeMsg);
}
