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
@Table(name = "order_books")
public class OrderBookJpaEntity extends BaseEntity {

    @Id
    private String id;
    private String baseCurrency;
    private String quoteCurrency;
}
