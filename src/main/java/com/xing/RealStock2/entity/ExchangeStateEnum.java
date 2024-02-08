package com.xing.RealStock2.entity;

public enum ExchangeStateEnum {
    CALL_AUCTION,CONTINUOUS_AUCTION,PAUSE,CLOSE;

    public static boolean isOpen(ExchangeStateEnum exchangeStateEnum){
        return exchangeStateEnum==CALL_AUCTION || exchangeStateEnum ==CONTINUOUS_AUCTION;
    }
}
