package com.xing.RealStock2.filter;

import com.xing.RealStock2.context.StockContext;
import com.xing.RealStock2.entity.AccountEntity;
import com.xing.RealStock2.entity.ResultEntity;
import com.xing.RealStock2.entity.TradeEntity;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(9)
public class MoneyFreezeFilter implements BuyFilter{
    @Override
    public ResultEntity doFilter(TradeEntity tradeEntity) {
        AccountEntity accountEntity= StockContext.getUserIdMap().get(tradeEntity.getUserId());
        return new ResultEntity(accountEntity.freezeMoney(tradeEntity.getPrice(),tradeEntity.getAmount()),"冻结资金");
    }
}
