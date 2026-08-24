package com.example.accountdemo.infrastructure.persistence.account;

import com.example.accountdemo.infrastructure.persistence.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
        name = "account_balances",
        uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "currency"})
)
/**
 * JPA entity bảng {@code account_balances} — số dư available/locked theo currency.
 *
 * <p><b>Vì sao cần class này:</b> map multi-currency wallet — nguồn sự thật available/locked.
 */
public class AccountBalanceJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountJpaEntity account;

    private String currency;
    private BigDecimal availableAmount;
    private BigDecimal lockedAmount;
}
