package com.xing.RealStock2.service;

import com.xing.RealStock2.context.StockContext;
import com.xing.RealStock2.engine.CallAuctionEngine;
import com.xing.RealStock2.engine.ContinuousAuctionEngine;
import com.xing.RealStock2.entity.AccountEntity;
import com.xing.RealStock2.entity.ExchangeStateEnum;
import com.xing.RealStock2.entity.StockEntity;
import com.xing.RealStock2.param.StockParam;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ScheduleService {

    private Thread thread;
    @Autowired
    private StockParam stockParam;

    @Autowired
    private CallAuctionEngine callAuctionEngine;

    @Autowired
    private ContinuousAuctionEngine continuousAuctionEngine;

    @PostConstruct
    public void init() {
        for (AccountEntity accountEntity : stockParam.getAccountEntities()) {
            accountEntity.init();
            StockContext.getUserIdMap().put(accountEntity.getUserId(), accountEntity);
        }
        for (StockEntity stockEntity : stockParam.getStockEntities()) {
            stockEntity.refresh();
            StockContext.getStockEntityMap().put(stockEntity.getStockCode(), stockEntity);
        }
        start();
    }

    public void start() {
        thread = new Thread(this::run);
        thread.start();
    }

    private void run() {
        while (true) {
            try {
                log.info("集合竞价1");
                callAuction1();
                Thread.sleep(10000);
                log.info("连续竞价");
                callContinuous();
                Thread.sleep(30000);
//                log.info("集合竞价2");
//                callAuction2();
//                Thread.sleep(10000);
//                log.info("结束当天交易");
//                finishDailyStock();
//                Thread.sleep(2000);

break;
//                Thread.sleep(999999);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void callAuction1() {
        StockContext.setExchangeStateEnum(ExchangeStateEnum.PAUSE);

        StockContext.dailyClear();
        StockContext.setExchangeStateEnum(ExchangeStateEnum.CALL_AUCTION);
    }

    private void callContinuous() throws InterruptedException {
        StockContext.setExchangeStateEnum(ExchangeStateEnum.PAUSE);
        Thread.sleep(1000);
        callAuctionEngine.matching();
        callAuctionEngine.moveToContinous();
        callAuctionEngine.dailyClear();
        StockContext.setExchangeStateEnum(ExchangeStateEnum.CONTINUOUS_AUCTION);
    }
    private void callAuction2() throws InterruptedException {
        StockContext.setExchangeStateEnum(ExchangeStateEnum.PAUSE);
        Thread.sleep(1000);
        continuousAuctionEngine.dailyClear();
        StockContext.setExchangeStateEnum(ExchangeStateEnum.CALL_AUCTION);
    }
    private void finishDailyStock() throws InterruptedException {
        StockContext.setExchangeStateEnum(ExchangeStateEnum.PAUSE);
        Thread.sleep(1000);
        callAuctionEngine.matching();
        callAuctionEngine.dailyClear();
        StockContext.dailyClear();
        StockContext.setExchangeStateEnum(ExchangeStateEnum.CLOSE);
    }
}
