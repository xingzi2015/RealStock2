package com.xing.RealStock2.param;

import com.xing.RealStock2.entity.AccountEntity;
import com.xing.RealStock2.entity.StockEntity;
import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "stock")
@Data
public class StockParam {
    private List<AccountEntity> accountEntities;

    private List<StockEntity> stockEntities;
}
