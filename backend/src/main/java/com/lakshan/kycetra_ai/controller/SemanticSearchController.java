package com.lakshan.kycetra_ai.controller;

import com.lakshan.kycetra_ai.dto.SemanticSearchResult;
import com.lakshan.kycetra_ai.service.SemanticSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class SemanticSearchController {

    private final SemanticSearchService searchService;

    public SemanticSearchController(
            SemanticSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "5") int limit,
            Authentication authentication) {

        try {
            List<SemanticSearchResult> results =
                    searchService.search(
                            authentication.getName(), query, limit);

            return ResponseEntity.ok(results);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", exception.getMessage()));
        }
    }
}
