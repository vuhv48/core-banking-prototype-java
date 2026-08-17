package com.example.accountdemo.api;

import com.example.accountdemo.api.dto.*;
import com.example.accountdemo.application.*;
import com.example.accountdemo.domain.account.model.Account;
import com.example.accountdemo.domain.exchange.order.model.Order;
import com.example.accountdemo.infrastructure.security.SecurityUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

/** REST API ví / tài khoản: xem số dư, nạp, rút, tạo mới. */
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final WithdrawApplicationService withdrawApplicationService;
    private final DepositApplicationService depositApplicationService;
    private final GetAccountApplicationService getAccountApplicationService;
    private final CreateAccountApplicationService createAccountApplicationService;
    private final ListOrdersByAccountApplicationService listOrdersByAccountApplicationService;
    private final TransferApplicationService transferApplicationService;

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

    @PostMapping
    public ResponseEntity<Void> createAccount(@RequestBody AccountRequest request) {
        createAccountApplicationService.createAccount(
                request.accountId(),
                request.status(),
                toInitialAvailable(request)
        );
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{accountId}/orders")
    public ResponseEntity<List<OrderResponse>> getOrdersByAccountId(@PathVariable String accountId) {
        List<Order> orders = listOrdersByAccountApplicationService.getOrdersByAccountId(
                SecurityUtils.currentUsername(),
                accountId
        );
        return ResponseEntity.ok(orders.stream().map(OrderResponse::from).toList());
    }

    /** API DTO → map currency/available cho Application (giống deposit/withdraw: không đẩy DTO xuống service). */
    private static Map<String, Long> toInitialAvailable(AccountRequest request) {
        Map<String, Long> map = new LinkedHashMap<>();
        if (request.holdings() == null) {
            return map;
        }
        for (HoldingRequestItem item : request.holdings()) {
            map.put(item.currency(), item.available());
        }
        return map;
    }

    @PostMapping("/transfer")
    public ResponseEntity<Void> transferMoney(@RequestBody TransferAccountRequest request) {
        transferApplicationService.transfer(SecurityUtils.currentUsername(),
                request.fromAccountId(),
                request.toAccountId(),
                request.amount(),
                request.currency()
        );
        return ResponseEntity.ok().build();
    }
}
