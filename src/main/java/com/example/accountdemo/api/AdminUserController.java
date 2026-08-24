package com.example.accountdemo.api;

import com.example.accountdemo.api.dto.AdminCreateUserRequest;
import com.example.accountdemo.api.dto.AdminCreateUserResponse;
import com.example.accountdemo.application.AdminCreateUserApplicationService;
import com.example.accountdemo.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API admin: tạo user login (ROLE_USER) + ví mới hoặc gắn account có sẵn.
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminCreateUserApplicationService adminCreateUserApplicationService;

    @PostMapping
    public ResponseEntity<AdminCreateUserResponse> createUser(@RequestBody AdminCreateUserRequest request) {
        var result = adminCreateUserApplicationService.createUser(
                SecurityUtils.currentUsername(),
                request.username(),
                request.password(),
                request.email(),
                request.accountId()
        );
        return ResponseEntity.ok(new AdminCreateUserResponse(
                result.username(),
                result.accountId(),
                "Tạo user thành công — có thể đăng nhập"
        ));
    }
}
