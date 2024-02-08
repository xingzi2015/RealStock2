package com.xing.RealStock2.notify;

import com.xing.RealStock2.entity.TradeMsg;
import com.xing.RealStock2.entity.TradeSideEnum;

public interface GlobalNotify {
    /**
     * 这里的 tradeSide 指被动成交的方向
     * @param tradeSide
     * @param tradeMsg
     */
    void notifyMatching(TradeSideEnum tradeSide, TradeMsg tradeMsg);

    void notifyMatching(String stockCode,Integer price);

    void continousNotifyWaitingMatch(TradeSideEnum tradeSide, TradeMsg tradeMsg);

    void callNotifyWaitingMatch( String stockCode,Integer price,Long buyAmount,Long sellAmount);

    void notifyCancel(TradeSideEnum tradeSide, TradeMsg tradeMsg);
}
