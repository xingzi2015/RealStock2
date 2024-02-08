package com.xing.RealStock2.filter;

import com.xing.RealStock2.entity.ResultEntity;
import com.xing.RealStock2.entity.TradeEntity;

public interface BuyFilter {
    ResultEntity doFilter(TradeEntity tradeEntity);
}
