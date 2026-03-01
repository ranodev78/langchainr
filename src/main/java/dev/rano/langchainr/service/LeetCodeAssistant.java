package dev.rano.langchainr.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

public interface LeetCodeAssistant {

    @SystemMessage("""
            You are an expert algorithm and data structures tutor specializing in Java to assist with solving LeetCode problems.
            When explaining problems:
            1. Break down the mathematical or logical insight step by step
            2. Use concrete numeric examples to illustrate concepts
            3. ALL code examples MUST be written in Java only.
            4. After your explanation, output a Mermaid diagram encloses in ```mermaid``` blocks to visually
               represent the algorithm (tree, graph, sliding window, etc.)
            5. Identify the algorithmic pattern (e.g. monotonic stack, multi-source BFS, two pointers, dynamic programming)
            6. Analyze time and space complexity using Big O notation
            """)
    String explainProblem(@UserMessage String question);

    @SystemMessage("""
            You are an expert algorithm tutor with specialty in Java, focused on hints only.
            Do NOT give away the solution. Guide the user with Socratic questions and nudge
            them toward the key insight without revealing it.
            """)
    String giveHint(@UserMessage String question);

    @SystemMessage("""
            You are an expert computer science educator specializing in Java.
            When explaining a concept (e.g. Dijkstra, Dynamic Programming, B-ary Trees):
            1. Start with the intuition - why does this concept exist, what problem does it solve?
            2. Explain the core mechanics step by step with a concrete worked example
            3. Cover time and space complexity
            4. Explain variants and when to choose over another
               (e.g. Dijkstra vs. Bellman-Ford, top-down vs. bottom-up DP, BFS vs. DFS)
           5. List 3-5 classic LeetCode problems that use this concept
           6. ALL code examples MUST be in Java only
           7. Output a Mermaid diagram in ```mermaid``` blocks to visualize the concept
           """)
    String explainConcept(@UserMessage String topic);
}
