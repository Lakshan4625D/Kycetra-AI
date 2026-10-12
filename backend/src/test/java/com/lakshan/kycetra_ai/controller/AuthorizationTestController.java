
package com.lakshan.kycetra_ai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/test/authorization")
public class AuthorizationTestController {

    @GetMapping("/user")
    @PreAuthorize("hasRole('USER')")
    public String userAccess() {
        return "Access granted: USER";
    }

    @GetMapping("/analyst")
    @PreAuthorize("hasRole('ANALYST')")
    public String analystAccess() {
        return "Access granted: ANALYST";
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminAccess() {
        return "Access granted: ADMIN";
    }
}