package com.xing.RealStock2.entity;

public enum ExchangeStateEnum {
    CALL_AUCTION,CONTINUOUS_AUCTION,PAUSE,CLOSE;

    public boolean isOpen(){
        return this==CALL_AUCTION || this ==CONTINUOUS_AUCTION;
    }
    public boolean isClose(){
        return this==CLOSE;
    }
}
