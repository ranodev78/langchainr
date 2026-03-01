package dev.rano.langchainr.controller;

import dev.rano.langchainr.dto.ExplainRequest;
import dev.rano.langchainr.dto.ReviewResult;
import dev.rano.langchainr.service.LeetCodeService;
import dev.rano.langchainr.service.reviewer.StreamingSynthesizerAssistant;
import jakarta.validation.Valid;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/leetcode")
@Validated
public class LeetCodeController {
    private static final Pattern REGEX_PATTERN = Pattern.compile("```mermaid\\n(.*?)```", Pattern.DOTALL);

    private final LeetCodeService leetCodeService;
    private final StreamingSynthesizerAssistant streamingSynthesizerAssistant;

    public LeetCodeController(final LeetCodeService leetCodeService,
                              final StreamingSynthesizerAssistant streamingSynthesizerAssistant) {
        this.leetCodeService = leetCodeService;
        this.streamingSynthesizerAssistant = streamingSynthesizerAssistant;
    }

    @GetMapping
    public String index(final Model model) {
        model.addAttribute("request", new ExplainRequest(""));
        return "index";
    }

    @PostMapping("/explain")
    public String explain(@ModelAttribute @Valid final ExplainRequest explainRequest,
                          final BindingResult result,
                          final Model model) {
        if (result.hasErrors()) {
            return "index";
        }

        final String response = this.leetCodeService.explainWithValidation(explainRequest.question());

        return processRequest(response, model, explainRequest);
    }

    @PostMapping("/concept")
    public String explainConcept(@ModelAttribute @Valid final ExplainRequest explainRequest,
                                 final BindingResult result,
                                 final Model model) {
        if (result.hasErrors()) {
            return "index";
        }

        final String response = this.leetCodeService.explainConcept(explainRequest.question());

        return processRequest(response, model, explainRequest);
    }

    @GetMapping(value = "/explain/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter explainStream(@RequestParam final String problem) {
        final SseEmitter emitter = new SseEmitter(180_000L);

        // Offload entire pipeline to a separate thread.
        // SseEmitter allows the HTTP thread to return immediately.
        CompletableFuture.runAsync(() -> {
            try {
                // Stage 1 -- blocking
                final String draft = this.leetCodeService.generateDraft(problem);

                // Stage 2 -- blocking
                final ReviewResult reviewResult = this.leetCodeService.runReview(problem, draft);

                if (LeetCodeService.isCleanPass(reviewResult)) {
                    emitter.send(SseEmitter.event().data(draft));
                    emitter.send(SseEmitter.event().name("complete").data(""));
                    emitter.complete();
                    return;
                }

                final String reviewText = reviewResult == null ? "" : reviewResult.toString();

                this.streamingSynthesizerAssistant.synthesize(draft, reviewText)
                        .onPartialResponse(token -> {
                            try {
                                emitter.send(SseEmitter.event().data(token));
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        })
                        .onCompleteResponse(response -> {
                            try {
                                emitter.send(SseEmitter.event().name("complete").data(""));
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        })
                        .onError(emitter::completeWithError)
                        .start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        return emitter;
    }

    private static String processRequest(final String response, final Model model, final ExplainRequest request) {
        final String mermaidDiagram = extractMermaid(response);
        final String explanation = stripMermaid(response);

        model.addAttribute("explanation", explanation);
        model.addAttribute("mermaidDiagram", mermaidDiagram);
        model.addAttribute("request", request);

        if (extractMermaid(response) == null) {
            System.out.print("Hello!");
        }

        return "index";
    }

    private static @Nullable String extractMermaid(final String response) {
        final Matcher matcher = REGEX_PATTERN.matcher(response);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private static @NonNull String stripMermaid(final String response) {
        return response.replaceAll("```mermaid\\n.*?```", "").trim();
    }
}
