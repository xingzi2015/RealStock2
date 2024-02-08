package com.xing.RealStock2.controller;

import com.xing.RealStock2.context.StockContext;
import com.xing.RealStock2.entity.AccountEntity;
import com.xing.RealStock2.entity.ResultEntity;
import com.xing.RealStock2.entity.TradeEntity;
import com.xing.RealStock2.service.TradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
public class UserController {
    @Autowired
    private TradeService tradeService;



    @GetMapping("/account/{userid}")
    public AccountEntity query(@PathVariable String userid){
        return StockContext.getUserIdMap().get(userid);
    }
}
