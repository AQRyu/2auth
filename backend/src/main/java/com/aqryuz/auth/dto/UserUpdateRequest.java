package com.aqryuz.auth.dto;

import java.util.Set;
import com.aqryuz.auth.entity.User;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequest {

    @Email(message = "Email should be valid")
    private String email;

    private String firstName;
    private String lastName;
    private String password;
    private Boolean accountEnabled;
    private Boolean accountLocked;
    private Set<User.Role> roles;
}
