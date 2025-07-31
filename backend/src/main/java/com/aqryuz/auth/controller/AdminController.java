package com.aqryuz.auth.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.aqryuz.auth.dto.UserCreateRequest;
import com.aqryuz.auth.dto.UserInfo;
import com.aqryuz.auth.dto.UserUpdateRequest;
import com.aqryuz.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    @GetMapping("/users")
    public ResponseEntity<Page<UserInfo>> getAllUsers(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<UserInfo> users = userService.getAllUsers(pageable);

        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserInfo> getUserById(@PathVariable Long id) {
        UserInfo user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/users")
    public ResponseEntity<UserInfo> createUser(@Valid @RequestBody UserCreateRequest request) {
        log.info("Admin creating new user: {}", request.getUsername());
        UserInfo user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<UserInfo> updateUser(@PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        log.info("Admin updating user with ID: {}", id);
        UserInfo user = userService.updateUser(id, request);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("Admin deleting user with ID: {}", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{id}/unlock")
    public ResponseEntity<UserInfo> unlockUser(@PathVariable Long id) {
        log.info("Admin unlocking user with ID: {}", id);
        UserUpdateRequest request = UserUpdateRequest.builder().accountLocked(false).build();
        UserInfo user = userService.updateUser(id, request);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/users/{id}/disable")
    public ResponseEntity<UserInfo> disableUser(@PathVariable Long id) {
        log.info("Admin disabling user with ID: {}", id);
        UserUpdateRequest request = UserUpdateRequest.builder().accountEnabled(false).build();
        UserInfo user = userService.updateUser(id, request);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/users/{id}/enable")
    public ResponseEntity<UserInfo> enableUser(@PathVariable Long id) {
        log.info("Admin enabling user with ID: {}", id);
        UserUpdateRequest request = UserUpdateRequest.builder().accountEnabled(true).build();
        UserInfo user = userService.updateUser(id, request);
        return ResponseEntity.ok(user);
    }
}
