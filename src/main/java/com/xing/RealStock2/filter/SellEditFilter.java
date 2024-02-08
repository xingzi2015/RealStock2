package com.xing.RealStock2.filter;

import com.xing.RealStock2.entity.ResultEntity;
import com.xing.RealStock2.entity.TradeEntity;
import com.xing.RealStock2.entity.TradeSideEnum;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class SellEditFilter implements SellFilter{
    @Override
    public ResultEntity doFilter(TradeEntity tradeEntity) {
        tradeEntity.setTradeSide(TradeSideEnum.SELL);
        return new ResultEntity(true);
    }
}
