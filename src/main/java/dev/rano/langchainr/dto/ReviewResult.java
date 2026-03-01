package dev.rano.langchainr.dto;

import org.jspecify.annotations.NonNull;

import java.util.List;

public record ReviewResult(
        boolean correctnessPassed,
        String correctnessNotes,

        // --- Complexity ---
        boolean complexityAccurate,
        String timeComplexity,
        String spaceComplexity,
        String complexityJustification,

        // --- Edge Cases ---
        List<String> edgeCasesHandled,
        List<String> edgeCasesMissed,

        // --- Readability ---
        List<String> readabilitySuggestions, // Naming, structure, clarity
        boolean overEngineered,

        // --- Enrichment (the "more context" you want) ---
        String problemPatternCategory,
        List<String> relatedPatterns,
        List<String> relatedProblems,
        String whyThisApproachWins,
        List<String> commonMistakes,
        String interviewTip,
        List<String> followUpVariants,
        String prerequisiteKnowledge,

        // --- Verdict ---
        String overallVerdict
) {

    @Override
    public @NonNull String toString() {
        return """
                VERDICT: %s
              
                CORRECTNESS: %s
                Notes: %s
              
                COMPLEXITY:
                    Time: %s -- %s
                    Space: %s
                  \s
                EDGE CASES HANDLED: %s
                EDGE CASES MISSED: %s
              
                READABILITY SUGGESTIONS: %s
                OVER-ENGINEERED: %s
               
                PATTERN: %s
                RELATED PATTERNS: %s
                RELATED PROBLEMS: %s
                WHY THIS APPROACHED WINS: %s
                COMMON MISTAKES: %s
                INTERVIEW TIP: %s
                FOLLOW UP VARIANTS: %s
                PREREQUISITE KNOWLEDGE: %s
               """
                .formatted(
                        overallVerdict,
                        correctnessPassed ? "PASS" : "FAIL",
                        correctnessNotes,
                        timeComplexity,
                        complexityJustification,
                        spaceComplexity,
                        String.join(", ", edgeCasesHandled),
                        String.join(", ", edgeCasesMissed),
                        String.join(", ", readabilitySuggestions),
                        overEngineered,
                        problemPatternCategory,
                        String.join(", ", relatedPatterns),
                        String.join(", ", relatedProblems),
                        whyThisApproachWins,
                        String.join(", ", commonMistakes),
                        interviewTip,
                        String.join(", ", followUpVariants),
                        prerequisiteKnowledge
                );
    }
}
