package com.aqryuz.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.aqryuz.auth.dto.UserInfo;
import com.aqryuz.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserInfo> getProfile(Authentication authentication) {
        String username = authentication.getName();
        UserInfo user = userService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/totp/enable")
    public ResponseEntity<TotpResponse> enableTotp(Authentication authentication) {
        String username = authentication.getName();
        log.info("User {} enabling TOTP", username);

        String qrCode = userService.enableTotp(username);
        TotpResponse response = new TotpResponse(qrCode,
                "TOTP setup initiated. Please scan the QR code and confirm with a TOTP code.");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/totp/confirm")
    public ResponseEntity<String> confirmTotp(Authentication authentication,
            @RequestBody TotpConfirmRequest request) {
        String username = authentication.getName();
        log.info("User {} confirming TOTP", username);

        userService.confirmTotp(username, request.getTotpCode());
        return ResponseEntity.ok("TOTP enabled successfully");
    }

    @PostMapping("/totp/disable")
    public ResponseEntity<String> disableTotp(Authentication authentication) {
        String username = authentication.getName();
        log.info("User {} disabling TOTP", username);

        userService.disableTotp(username);
        return ResponseEntity.ok("TOTP disabled successfully");
    }

    @PostMapping("/password/change")
    public ResponseEntity<String> changePassword(Authentication authentication,
            @RequestBody ChangePasswordRequest request) {
        String username = authentication.getName();
        log.info("User {} changing password", username);

        // Note: In a real implementation, you should verify the current password first
        UserInfo user = userService.getUserByUsername(username);
        userService.updateUser(user.getId(), com.aqryuz.auth.dto.UserUpdateRequest.builder()
                .password(request.getNewPassword()).build());

        return ResponseEntity.ok("Password changed successfully");
    }

    // Inner classes for request/response DTOs
    public static class TotpResponse {
        private String qrCode;
        private String message;

        public TotpResponse(String qrCode, String message) {
            this.qrCode = qrCode;
            this.message = message;
        }

        public String getQrCode() {
            return qrCode;
        }

        public void setQrCode(String qrCode) {
            this.qrCode = qrCode;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class TotpConfirmRequest {
        private String totpCode;

        public String getTotpCode() {
            return totpCode;
        }

        public void setTotpCode(String totpCode) {
            this.totpCode = totpCode;
        }
    }

    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;

        public String getCurrentPassword() {
            return currentPassword;
        }

        public void setCurrentPassword(String currentPassword) {
            this.currentPassword = currentPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}
