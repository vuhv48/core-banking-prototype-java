package com.example.accountdemo.api;

import com.example.accountdemo.api.dto.AccountResponse;
import com.example.accountdemo.application.DepositApplicationService;
import com.example.accountdemo.application.GetAccountApplicationService;
import com.example.accountdemo.application.WithdrawApplicationService;
import com.example.accountdemo.domain.account.model.Account;
import com.example.accountdemo.infrastructure.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final WithdrawApplicationService withdrawApplicationService;
    private final DepositApplicationService depositApplicationService;
    private final GetAccountApplicationService getAccountApplicationService;

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> get(@PathVariable String accountId) {
        Account account = getAccountApplicationService.get(SecurityUtils.currentUsername(), accountId);
        return ResponseEntity.ok(AccountResponse.from(account));
    }

    @PostMapping("/{accountId}/withdraw")
    public ResponseEntity<Void> withdraw(
            @PathVariable String accountId,
            @RequestBody AmountRequest request
    ) {
        withdrawApplicationService.withdraw(accountId, request.amount(), request.currency());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<Void> deposit(
            @PathVariable String accountId,
            @RequestBody AmountRequest request
    ) {
        depositApplicationService.deposit(accountId, request.amount(), request.currency());
        return ResponseEntity.ok().build();
    }

    public record AmountRequest(long amount, String currency) {
    }
}
