package com.aqryuz.auth.dto;

import java.time.LocalDateTime;
import java.util.Set;
import com.aqryuz.auth.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User information")
public class UserInfo {

    @Schema(description = "User ID", example = "1")
    private Long id;

    @Schema(description = "Username", example = "admin")
    private String username;

    @Schema(description = "Email address", example = "admin@homelab.local")
    private String email;

    @Schema(description = "First name", example = "System")
    private String firstName;

    @Schema(description = "Last name", example = "Administrator")
    private String lastName;

    @Schema(description = "Full name", example = "System Administrator")
    private String fullName;

    @Schema(description = "Whether TOTP is enabled for this user", example = "false")
    private boolean totpEnabled;

    @Schema(description = "Whether account is enabled", example = "true")
    private boolean accountEnabled;

    @Schema(description = "Whether account is locked", example = "false")
    private boolean accountLocked;

    @Schema(description = "Last login timestamp", example = "2025-07-31T12:30:00")
    private LocalDateTime lastLogin;

    @Schema(description = "Account creation timestamp", example = "2025-07-30T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "User roles", example = "[\"ADMIN\"]")
    private Set<User.Role> roles;

    public static UserInfo fromUser(User user) {
        return UserInfo.builder().id(user.getId()).username(user.getUsername())
                .email(user.getEmail()).firstName(user.getFirstName()).lastName(user.getLastName())
                .fullName(user.getFullName()).totpEnabled(user.isTotpEnabled())
                .accountEnabled(user.isAccountEnabled()).accountLocked(user.isAccountLocked())
                .lastLogin(user.getLastLogin()).createdAt(user.getCreatedAt())
                .roles(user.getRoles()).build();
    }
}
