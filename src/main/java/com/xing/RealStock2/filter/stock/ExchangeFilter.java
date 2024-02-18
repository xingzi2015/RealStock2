package com.xing.RealStock2.filter.stock;

import com.xing.RealStock2.context.StockContext;
import com.xing.RealStock2.entity.ExchangeStateEnum;
import com.xing.RealStock2.entity.ResultEntity;
import com.xing.RealStock2.entity.TradeEntity;
import org.springframework.stereotype.Component;

@Component
public class ExchangeFilter implements StockFilter{
    @Override
    public ResultEntity doFilter(TradeEntity tradeEntity) {
        if(StockContext.getExchangeStateEnum().isOpen()){
            return new ResultEntity(true);
        }
        return new ResultEntity(false,"交易暂停");
    }
}
