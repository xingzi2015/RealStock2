package com.xing.RealStock2.engine;

import com.xing.RealStock2.entity.ResultEntity;
import com.xing.RealStock2.entity.TradeEntity;

public interface AuctionEngine {
    ResultEntity buy(TradeEntity buyTradeEntity);
    ResultEntity sell(TradeEntity sellTradeEntity);
}
