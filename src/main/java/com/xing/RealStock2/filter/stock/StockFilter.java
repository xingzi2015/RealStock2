package com.xing.RealStock2.filter.stock;

import com.xing.RealStock2.entity.ResultEntity;
import com.xing.RealStock2.entity.TradeEntity;

public interface StockFilter {
    ResultEntity doFilter(TradeEntity tradeEntity);
}
