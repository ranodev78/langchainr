package dev.rano.langchainr.service;

import dev.rano.langchainr.dto.ReviewResult;
import dev.rano.langchainr.service.rag.ExplanationRetriever;
import dev.rano.langchainr.service.reviewer.SolutionReviewerAssistant;
import dev.rano.langchainr.service.reviewer.SolutionSynthesizerAssistant;
import dev.rano.langchainr.util.CacheKeyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class LeetCodeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LeetCodeService.class);

    private static final String RETRIEVER_TEMPLATE = """
            Relevant context from previous explanations:
            %s
            
            Question: %s
            """;

    private final LeetCodeAssistant leetCodeAssistant;
    private final CacheKeyUtil cacheKeyUtil;
    private final ExplanationRetriever explanationRetriever;
    private final SolutionReviewerAssistant solutionReviewerAssistant;
    private final SolutionSynthesizerAssistant solutionSynthesizerAssistant;

    @Autowired
    public LeetCodeService(final LeetCodeAssistant leetCodeAssistant,
                           final CacheKeyUtil cacheKeyUtil,
                           final ExplanationRetriever explanationRetriever,
                           final SolutionReviewerAssistant solutionReviewerAssistant,
                           final SolutionSynthesizerAssistant solutionSynthesizerAssistant) {
        this.leetCodeAssistant = leetCodeAssistant;
        this.cacheKeyUtil = cacheKeyUtil;
        this.explanationRetriever = explanationRetriever;
        this.solutionReviewerAssistant = solutionReviewerAssistant;
        this.solutionSynthesizerAssistant = solutionSynthesizerAssistant;
    }

    @Cacheable(value = "leetcode-explanations", key = "@cacheKeyUtil.normalize(#question)")
    public String explainWithValidation(final String question) {
        // Stage 1 -- draft from existing RAG-enriched assistant
        final String draft = this.leetCodeAssistant.explainProblem(question);

        // Stage 2 -- structured review
        final ReviewResult reviewResult = this.solutionReviewerAssistant.review(question, draft);

        // Short-circuit: if clean pass with no flags, skip synthesis
        if ("PASS".equals(reviewResult.overallVerdict()) &&
                reviewResult.edgeCasesMissed().isEmpty() &&
                reviewResult.readabilitySuggestions().isEmpty()) {
            return draft;
        }

        return this.solutionSynthesizerAssistant.synthesize(draft, reviewResult.toString());
    }

    private String runExplanationPipeline(final String problem) {
        // Stage 1: RAG-enriched draft
        final String draft = this.generateDraft(problem);

        // Stage 2: Structured review
        final ReviewResult reviewResult = this.runReview(problem, draft);
        if (reviewResult == null) {
            LOGGER.warn("Review stage failed for '{}', falling back to draft", problem);
            return draft;
        }

        LOGGER.info("Review verdict for '{}': {} | edgeCasesMissed={} | readabilitySuggestions={}",
                    problem,
                    reviewResult.overallVerdict(),
                    reviewResult.edgeCasesMissed().size(),
                    reviewResult.readabilitySuggestions().size());

        // Stage 3.a: Short-circuit if no issues found
        if (isCleanPass(reviewResult)) {
            LOGGER.info("Clean pass -- skipping synthesis for '{}'", problem);
            return draft;
        }

        // Stage 3.b: Synthesize enriched final output
        return this.runSynthesize(problem, draft);
    }

    public String generateDraft(final String problem) {
        return this.leetCodeAssistant.explainProblem(problem);
    }

    public ReviewResult runReview(final String problem, final String draft) {
        try {
            return this.solutionReviewerAssistant.review(problem, draft);
        } catch (Exception ex) {
            LOGGER.error("SolutionReviewerAssistant failed for '{}': {}", problem, ex.getMessage(), ex);
            return null;
        }
    }

    public static boolean isCleanPass(final ReviewResult reviewResult) {
        return "PASS".equals(reviewResult.overallVerdict())
                && reviewResult.edgeCasesMissed().isEmpty()
                && reviewResult.readabilitySuggestions().isEmpty()
                && reviewResult.correctnessPassed()
                && reviewResult.complexityAccurate();
    }

    private String runSynthesize(final String problem, final String draft) {
        try {
            return this.solutionSynthesizerAssistant.synthesize(draft, problem);
        } catch (Exception ex) {
            LOGGER.error("SolutionSynthesizer failed for '{}': {}", problem, ex.getMessage(), ex);
            return null;
        }
    }

    @Cacheable(value = "concept-explanations", key = "@cacheKeyUtil.normalize(#topic)")
    public String explainConcept(final String topic) {
        return this.leetCodeAssistant.explainConcept(this.buildPrompt(topic));
    }

    @Cacheable(value = "hints", key = "@cacheKeyUtil.normalize(#question)")
    public String giveHint(final String question) {
        return this.leetCodeAssistant.giveHint(this.buildPrompt(question));
    }

    @CacheEvict(allEntries = true, value = { "leetcode-explanations", "concept-explanations", "hints" })
    public void clearAllCaches() {}

    private String buildPrompt(final String input) {
        return this.explanationRetriever.retrieve(input)
                .map(context -> RETRIEVER_TEMPLATE.formatted(context, input))
                .orElse(input);
    }
}
