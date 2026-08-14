package com.example.accountdemo.application;

import com.example.accountdemo.domain.account.model.Account;
import com.example.accountdemo.domain.account.AccountRepository;
import com.example.accountdemo.domain.account.model.AccountStatus;
import com.example.accountdemo.domain.account.model.Money;
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
class DepositApplicationServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private DepositApplicationService depositApplicationService;

    @Test
    void deposit_shouldLoadAccountDepositAndSave() {
        Account account = new Account("ACC-001", new Money(100_000, "VND"), AccountStatus.ACTIVE);
        when(accountRepository.findById("ACC-001")).thenReturn(account);

        depositApplicationService.deposit("ACC-001", 50_000, "VND");

        assertEquals(150_000, account.getBalance().getAmount());
        verify(accountRepository).save(account);
    }

    @Test
    void deposit_shouldThrowWhenAccountNotFound() {
        when(accountRepository.findById("UNKNOWN")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> depositApplicationService.deposit("UNKNOWN", 10_000, "VND"));

        verify(accountRepository, never()).save(any());
    }
}
