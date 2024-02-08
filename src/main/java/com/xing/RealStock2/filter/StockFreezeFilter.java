package com.xing.RealStock2.filter;

import com.xing.RealStock2.context.StockContext;
import com.xing.RealStock2.entity.AccountEntity;
import com.xing.RealStock2.entity.ResultEntity;
import com.xing.RealStock2.entity.TradeEntity;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(9)
public class StockFreezeFilter implements SellFilter{
    @Override
    public ResultEntity doFilter(TradeEntity tradeEntity) {
        AccountEntity accountEntity= StockContext.getUserIdMap().get(tradeEntity.getUserId());
        return new ResultEntity(accountEntity.freezeStock(tradeEntity.getStockCode(),tradeEntity.getAmount()),"冻结股票");
    }
}
