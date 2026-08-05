package com.example.accountdemo.api;

import com.example.accountdemo.application.DepositApplicationService;
import com.example.accountdemo.application.WithdrawApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final WithdrawApplicationService withdrawApplicationService;
    private final DepositApplicationService depositApplicationService;

    public AccountController(
            WithdrawApplicationService withdrawApplicationService,
            DepositApplicationService depositApplicationService
    ) {
        this.withdrawApplicationService = withdrawApplicationService;
        this.depositApplicationService = depositApplicationService;
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
