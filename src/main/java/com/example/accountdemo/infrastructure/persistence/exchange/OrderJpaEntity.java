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
@Table(name = "orders")
public class OrderJpaEntity extends BaseEntity {

    @Id
    private String id;
    private String accountId;
    private String side;
    private String orderType;
    private String baseCurrency;
    private String quoteCurrency;
    private long quantity;
    private Long price;
    private long filledQuantity;
    private String status;
    private String lockedCurrency;
    private long lockedAmountRemaining;
}
