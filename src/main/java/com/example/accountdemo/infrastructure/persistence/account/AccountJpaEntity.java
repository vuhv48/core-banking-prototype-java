package com.example.accountdemo.infrastructure.persistence.account;

import com.example.accountdemo.infrastructure.persistence.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "accounts")
public class AccountJpaEntity extends BaseEntity {

    @Id
    private String id;
    private String status;

    /** Legacy columns — giữ để tương thích DB cũ; nguồn sự thật là account_balances. */
    private Long balanceAmount;
    private String balanceCurrency;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<AccountBalanceJpaEntity> balances = new ArrayList<>();
}
