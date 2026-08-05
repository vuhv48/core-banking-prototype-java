package com.example.accountdemo.infrastructure.persistence.account;

import com.example.accountdemo.infrastructure.persistence.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "accounts")
public class AccountJpaEntity extends BaseEntity {

    @Id
    private String id;
    private long balanceAmount;
    private String balanceCurrency;
    private String status;
}
