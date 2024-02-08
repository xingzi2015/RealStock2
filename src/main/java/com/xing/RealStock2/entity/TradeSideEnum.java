package com.xing.RealStock2.entity;

import java.util.Random;

public enum TradeSideEnum {
    BUY,SELL;

    public static TradeSideEnum getRandomTradeSide() {
        Random random = new Random();
        return values()[random.nextInt(values().length)];
    }
}
