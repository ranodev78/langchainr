package dev.rano.langchainr.service.reviewer;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface SolutionSynthesizerAssistant {

    @SystemMessage("""
            You are a senior technical writer and algorithm expert. \
            You will receive an original explanation of a LeetCode solution \
            and a structured review of that explanation.
            
            Your job is to produce the final, authoritative explanation by:
            - Correcting any errors identified in the review.
            - Incorporating all enrichment context (patterns, related problems, \
            interview tips, follow-ups, prerequisites).
            - Preserving the original structure where it was correct.
            - Writing clearly for an intermediate developer -- no hand-holding, \
            but no unexplained leaps either.
            
            Do NOT mention that is is a revised version. \
            Do NOT reference the review process. \
            Output only the final polished explanation.
            """)
    @UserMessage("""
            Original explanation:
            {{explanation}}
            
            Structured review:
            {{review}}
            
            Produce the final enriched explanation.
            """)
    String synthesize(@V("explanation") String explanation, @V("review") String review);
}
