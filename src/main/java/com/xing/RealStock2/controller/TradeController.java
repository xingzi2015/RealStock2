package com.xing.RealStock2.controller;

import com.xing.RealStock2.context.StockContext;
import com.xing.RealStock2.entity.*;
import com.xing.RealStock2.service.TradeService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
public class TradeController {
    @Autowired
    private TradeService tradeService;

    @PostMapping("/buy")
    public ResultEntity buy(@RequestBody TradeEntity tradeEntity){
        return tradeService.buy(tradeEntity);
    }

    @PostMapping("/sell")
    public ResultEntity sell(@RequestBody TradeEntity tradeEntity){
        return tradeService.sell(tradeEntity);
    }

    @PostMapping("/cancel")
    public ResultEntity sell(@RequestParam String userId,@RequestParam String uuid){
        return tradeService.cancel(uuid,userId);
    }

    @GetMapping("/topN")
    public TradeTopNEntity topN(@RequestParam String stockCode, @RequestParam Integer n){
        return StockContext.fetchTradeTopN(stockCode,n);
    }

    @GetMapping("/price")
    public StockEntity price(@RequestParam String stockCode){
        return StockContext.getStockEntityMap().get(stockCode);
    }
    @GetMapping("/transaction")
    public TransactionListEntity transaction(@RequestParam String stockCode, @RequestParam Integer start){
        return StockContext.fetchTradeMsg(stockCode,start);
    }
}
