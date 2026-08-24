package com.example.accountdemo.infrastructure.persistence.exchange;

import com.example.accountdemo.infrastructure.persistence.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * JPA entity bảng {@code orders}.
 *
 * <p><b>Vì sao cần class này:</b> persistence lệnh (kể cả FILLED/CANCELLED); domain Order không gắn JPA.
 */
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
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal filledQuantity;
    private String status;
    private String lockedCurrency;
    private BigDecimal lockedAmountRemaining;
}
