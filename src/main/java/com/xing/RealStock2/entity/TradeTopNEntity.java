package com.xing.RealStock2.entity;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class TradeTopNEntity {
    private Map<Integer,Long> buyMap;
    private Map<Integer,Long> sellMap;
}
