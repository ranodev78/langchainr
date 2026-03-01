package dev.rano.langchainr.controller;

import dev.rano.langchainr.service.ExplanationExportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Path;

@RestController
@RequestMapping("/leetcode")
public class ExportController {
    private final ExplanationExportService explanationExportService;

    public ExportController(final ExplanationExportService explanationExportService) {
        this.explanationExportService = explanationExportService;
    }

    @PostMapping("/save")
    public ResponseEntity<String> save(@RequestParam final String title,
                                       @RequestParam final String content,
                                       @RequestParam(required = false, defaultValue = "") final String mermaidDiagram) {
        try {
            final Path saved = this.explanationExportService.save(title, content, mermaidDiagram);
            return ResponseEntity.ok(saved.toString());
        } catch (IOException ex) {
            return ResponseEntity.internalServerError()
                    .body("Failed to save: " + ex.getMessage());
        }
    }
}
