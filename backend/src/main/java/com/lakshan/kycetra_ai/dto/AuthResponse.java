
package com.lakshan.kycetra_ai.dto;

public record AuthResponse(
    Long id,
    String username,
    String email,
    String message
) {}