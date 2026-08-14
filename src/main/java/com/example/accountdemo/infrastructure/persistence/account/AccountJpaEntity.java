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

/**
 * JPA entity bảng {@code accounts} — trạng thái ví + danh sách balances.
 *
 * <p><b>Vì sao cần class này:</b> persistence của aggregate Account; tách khỏi domain model thuần.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "accounts")
public class AccountJpaEntity extends BaseEntity {

    @Id
    private String id;
    private String status;

    /** Số dư theo currency — nguồn sự thật (bảng account_balances). */
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<AccountBalanceJpaEntity> balances = new ArrayList<>();
}
