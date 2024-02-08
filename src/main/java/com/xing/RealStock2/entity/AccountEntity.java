package com.xing.RealStock2.entity;

import com.xing.RealStock2.common.DailyClear;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
@Slf4j
public class AccountEntity implements DailyClear {
    private String userId;
    private Long money;
    private Long freezeMoney=0L;
    private Map<String,Long> stockAmountMap=new ConcurrentHashMap<>();
    private Map<String,Long> freezeStockAmountMap=new ConcurrentHashMap<>();
    private Map<String,TradeEntity> matchingTradeEntityMap=new ConcurrentHashMap<>();


    private List<String> initStocks;

    public void init(){
        initStocks.stream().forEach(str->{
            String[] arr=str.split(",");
            stockAmountMap.put(arr[0],Long.parseLong(arr[1]));
        });
    }

    public synchronized boolean freezeMoney(Integer stockPrice, Long quantity) {
        Long totalFreezeMoney = stockPrice * quantity;

        // 检查账户余额是否足够
        if (money.compareTo(totalFreezeMoney) < 0) {
            return false; // 冻结失败，余额不足
        }

        money -= totalFreezeMoney;
        freezeMoney += totalFreezeMoney;
        log.info("冻结资金，用户：{}，价格：{}，数量：{}",userId,stockPrice,quantity);
        return true; // 冻结成功
    }

    public synchronized void unfreezeMoney(Integer stockPrice, Long quantity) {
        long totalUnfreezeMoney = stockPrice * quantity;


        if (freezeMoney.compareTo(totalUnfreezeMoney)<0) {
            log.warn("解冻资金失败，用户：{}，价格：{}，数量：{}",userId,stockPrice,quantity);
            return;
        }

        money += totalUnfreezeMoney;
        freezeMoney -= totalUnfreezeMoney;
        log.info("解冻资金，用户：{}，金额：{}",userId,totalUnfreezeMoney);
    }

    public synchronized void moneyToStock(TradeMsg tradeMsg) {
        long totalUnfreezeMoney = tradeMsg.getIntendPrice() * tradeMsg.getAmount();
        long totalTransactionMoney = tradeMsg.getTransactionPrice() * tradeMsg.getAmount();
        String stockCode = tradeMsg.getStockCode();
        if (freezeMoney.compareTo(totalUnfreezeMoney)<0) {
            log.warn("转股失败，用户：{}，交易信息：{}",userId,tradeMsg);
            return;
        }
        freezeMoney -= totalUnfreezeMoney;
        money += (totalUnfreezeMoney-totalTransactionMoney);
        stockAmountMap.putIfAbsent(stockCode,0L);
        stockAmountMap.put(stockCode,stockAmountMap.get(stockCode)+tradeMsg.getAmount());
        log.info("转股金额，用户：{}，交易信息：{}",userId,tradeMsg);

    }

    public synchronized boolean freezeStock(String stockCode, Long quantity) {

        // 检查账户余额是否足够
        if (stockAmountMap.get(stockCode).compareTo(quantity) < 0) {
            return false; // 冻结失败，余额不足
        }
        stockAmountMap.putIfAbsent(stockCode,0L);
        freezeStockAmountMap.putIfAbsent(stockCode,0L);

        stockAmountMap.put(stockCode,stockAmountMap.get(stockCode)-quantity);
        freezeStockAmountMap.put(stockCode,freezeStockAmountMap.get(stockCode)+quantity);

        return true; // 冻结成功
    }

    public synchronized void unfreezeStock(String stockCode, Long quantity) {

        // 检查账户余额是否足够
        if (stockAmountMap.get(stockCode).compareTo(quantity) < 0) {
            log.warn("解冻失败，用户：{}，代码：{}，数量：{}",userId,stockCode,quantity);
            return; // 冻结失败，余额不足
        }
        stockAmountMap.putIfAbsent(stockCode,0L);
        freezeStockAmountMap.putIfAbsent(stockCode,0L);

        stockAmountMap.put(stockCode,stockAmountMap.get(stockCode)+quantity);
        freezeStockAmountMap.put(stockCode,freezeStockAmountMap.get(stockCode)-quantity);

    }

    public synchronized void stockToMoney(TradeMsg tradeMsg) {
        long totalMoney = tradeMsg.getTransactionPrice() * tradeMsg.getAmount();

        String stockCode = tradeMsg.getStockCode();
        if (freezeStockAmountMap.get(stockCode).compareTo(tradeMsg.getAmount())<0) {
            log.warn("转钱失败，用户：{}，交易信息：{}",userId,tradeMsg);
            return;
        }
        money += totalMoney;
        freezeStockAmountMap.put(stockCode,freezeStockAmountMap.get(stockCode)-tradeMsg.getAmount());

    }

    @Override
    public void dailyClear(){
        matchingTradeEntityMap.clear();
    }
}
