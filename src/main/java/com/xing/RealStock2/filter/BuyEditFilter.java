package com.xing.RealStock2.filter;

import com.xing.RealStock2.context.StockContext;
import com.xing.RealStock2.entity.AccountEntity;
import com.xing.RealStock2.entity.ResultEntity;
import com.xing.RealStock2.entity.TradeEntity;
import com.xing.RealStock2.entity.TradeSideEnum;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class BuyEditFilter implements BuyFilter{
    @Override
    public ResultEntity doFilter(TradeEntity tradeEntity) {
        tradeEntity.setTradeSide(TradeSideEnum.BUY);
        return new ResultEntity(true);
    }
}
