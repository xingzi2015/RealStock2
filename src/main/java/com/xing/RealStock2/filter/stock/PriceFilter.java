package com.xing.RealStock2.filter.stock;

import com.xing.RealStock2.context.StockContext;
import com.xing.RealStock2.entity.ResultEntity;
import com.xing.RealStock2.entity.StockEntity;
import com.xing.RealStock2.entity.TradeEntity;
import org.springframework.stereotype.Component;

@Component
public class PriceFilter implements StockFilter{
    private static final Integer limitRate=10;
    @Override
    public ResultEntity doFilter(TradeEntity tradeEntity) {
        StockEntity stockEntity=StockContext.getStockEntityMap().get(tradeEntity.getStockCode());


        if (stockEntity != null) {
            Integer lastPrice = stockEntity.getLastPrice();
            Integer upperLimit = lastPrice*(100+limitRate)/100;
            Integer lowerLimit =lastPrice*(100-limitRate)/100;
            //主动买场景
            if(tradeEntity.isActiveBuy()){
                tradeEntity.setPrice(upperLimit);
                return new ResultEntity(true);
            }
            Integer currentPrice = tradeEntity.getPrice();
            if (currentPrice != null) {
                // 检查价格是否超过昨日价格的1.1倍或者低于昨日价格的0.9倍
                if (currentPrice.compareTo(upperLimit) > 0 || currentPrice.compareTo(lowerLimit) < 0) {
                    return new ResultEntity(false,"成交价过高或过低"+currentPrice); // 不满足过滤条件，返回false
                }
            }
        }else {
            throw new IllegalStateException("不存在当前股票代码："+tradeEntity.getStockCode());
        }
        return new ResultEntity(true); // 满足过滤条件，返回true
    }
}
