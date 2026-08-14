package com.example.accountdemo.application;

import com.example.accountdemo.domain.account.model.Account;
import com.example.accountdemo.domain.account.AccountRepository;
import com.example.accountdemo.domain.account.model.AccountStatus;
import com.example.accountdemo.domain.account.model.Balance;
import com.example.accountdemo.domain.account.model.Money;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WithdrawApplicationServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private WithdrawApplicationService withdrawApplicationService;

    @Test
    void withdraw_shouldLoadAccountWithdrawAndSave() {
        Map<String, Balance> holdings = new LinkedHashMap<>();
        holdings.put("VND", new Balance("VND", 100_000, 0));
        Account account = new Account("ACC-001", AccountStatus.ACTIVE, holdings);
        when(accountRepository.findById("ACC-001")).thenReturn(account);

        withdrawApplicationService.withdraw("ACC-001", 30_000, "VND");

        assertEquals(70_000, account.getAvailable("VND").getAmount());
        verify(accountRepository).save(account);
    }

    @Test
    void withdraw_shouldThrowWhenAccountNotFound() {
        when(accountRepository.findById("UNKNOWN")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> withdrawApplicationService.withdraw("UNKNOWN", 10_000, "VND"));

        verify(accountRepository, never()).save(any());
    }
}
