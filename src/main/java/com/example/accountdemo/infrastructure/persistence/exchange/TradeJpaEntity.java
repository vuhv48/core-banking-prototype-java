package com.example.accountdemo.infrastructure.persistence.exchange;

import com.example.accountdemo.infrastructure.persistence.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "trades")
public class TradeJpaEntity extends BaseEntity {

    @Id
    private String id;
    private String buyOrderId;
    private String sellOrderId;
    private String buyerAccountId;
    private String sellerAccountId;
    private String baseCurrency;
    private String quoteCurrency;
    private long quantity;
    private long price;
}
