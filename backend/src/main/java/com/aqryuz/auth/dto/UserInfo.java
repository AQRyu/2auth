package com.aqryuz.auth.dto;

import java.time.LocalDateTime;
import java.util.Set;
import com.aqryuz.auth.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfo {

    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private boolean totpEnabled;
    private boolean accountEnabled;
    private boolean accountLocked;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
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
