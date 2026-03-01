package dev.rano.langchainr.util;

import dev.rano.langchainr.service.LeetCodeProblemRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public final class CacheKeyUtil {
    private final LeetCodeProblemRegistry registry;

    @Autowired
    public CacheKeyUtil(final LeetCodeProblemRegistry registry) {
        this.registry = registry;
    }

    private static final Set<String> STOP_WORDS = Set.of(
            "explain", "what", "is", "the", "a", "an", "how", "does",
            "can", "you", "me", "please", "describe", "tell", "about",
            "leetcode", "problem", "question", "algorithm", "data",
            "structure", "i", "want", "to", "understand", "learn",
            "teach", "show", "walk", "through", "give", "help", "with",
            "solve", "need", "my", "in", "of", "for", "and", "or",
            "more", "detail", "detailed", "deep", "dive", "overview",
            "intro", "introduction", "basics", "concept"
    );

    // Maps common aliases/variations to a canonical cache key
    private static final Map<String, String> CONCEPT_ALIASES = Map.<String, String>ofEntries(
            // Graph algorithms
            Map.entry("dijkstra", "dijkstra-shortest-path"),
            Map.entry("bellman", "bellman-ford"),
            Map.entry("bellman-ford", "bellman-ford"),
            Map.entry("floyd", "floyd-warshall"),
            Map.entry("warshall", "floyd-warshall"),
            Map.entry("kruskal", "kruskal-mst"),
            Map.entry("prim", "prim-mst"),
            Map.entry("topological", "topological-sort"),
            Map.entry("tarjan", "tarjan-scc"),
            Map.entry("kosaraju", "kosaraju-scc"),

            // Search / traversal
            Map.entry("bfs", "breadth-first-search"),
            Map.entry("dfs", "depth-first-search"),
            Map.entry("binary-search", "binary-search"),
            Map.entry("breadth-first-search", "breadth-first-search"),
            Map.entry("depth-first-search", "depth-first-search"),

            // Sorting
            Map.entry("quicksort", "quicksort"),
            Map.entry("mergesort", "mergesort"),
            Map.entry("heapsort", "heapsort"),
            Map.entry("timsort", "timsort"),
            Map.entry("counting-sort", "counting-sort"),
            Map.entry("radix-sort", "radix-sort"),
            Map.entry("bucket-sort", "bucket-sort"),

            // Data structures
            Map.entry("trie", "trie"),
            Map.entry("segment-tree", "segment-tree"),
            Map.entry("fenwick", "fenwick-tree"),
            Map.entry("bit", "fenwick-tree"),           // Binary Indexed Tree alias
            Map.entry("union-find", "union-find"),
            Map.entry("disjoint-set", "union-find"),
            Map.entry("monotonic-stack", "monotonic-stack"),
            Map.entry("deque", "deque"),
            Map.entry("heap", "heap"),
            Map.entry("avl", "avl-tree"),
            Map.entry("red-black", "red-black-tree"),
            Map.entry("b-tree", "b-tree"),
            Map.entry("skip-list", "skip-list"),

            // Techniques / paradigms
            Map.entry("dynamic-programming", "dynamic-programming"),
            Map.entry("dp", "dynamic-programming"),
            Map.entry("memoization", "dynamic-programming"),
            Map.entry("tabulation", "dynamic-programming"),
            Map.entry("greedy", "greedy"),
            Map.entry("backtracking", "backtracking"),
            Map.entry("divide-and-conquer", "divide-and-conquer"),
            Map.entry("two-pointers", "two-pointers"),
            Map.entry("sliding-window", "sliding-window"),
            Map.entry("kadane", "kadane"),
            Map.entry("kmp", "kmp-string-matching"),
            Map.entry("rabin-karp", "rabin-karp"),
            Map.entry("z-algorithm", "z-algorithm"),

            // Multi-source BFS — you encountered this in your interview!
            Map.entry("multi-source", "multi-source-bfs"),
            Map.entry("multisource", "multi-source-bfs"),

            // Sorting aliases
            Map.entry("quick-sort", "quicksort"),
            Map.entry("merge-sort", "mergesort"),
            Map.entry("heap-sort", "heapsort"),

            // Common shorthand
            Map.entry("lcs", "longest-common-subsequence"),
            Map.entry("lis", "longest-increasing-subsequence"),
            Map.entry("mst", "minimum-spanning-tree"),
            Map.entry("scc", "strongly-connected-components"),

            // Two pointers variations
            Map.entry("two-pointer", "two-pointers"),  // singular vs plural

            // Binary search variations
            Map.entry("bs", "binary-search")
    );

    private static final Pattern LEETCODE_NUMBER = Pattern.compile("\\b(\\d{1,4})\\b");

    public String normalize(final String input) {
        final String cleaned = input.toLowerCase().trim();

        // Strategy 1: LeetCode problem number present -> use it directly
        final Matcher numberMatcher = LEETCODE_NUMBER.matcher(cleaned);
        if (numberMatcher.find()) {
            return "lc-" + numberMatcher.group(1);
        }

        // Strategy 2 (new)
        final Optional<String> registryMatch = this.registry.resolve(cleaned);
        if (registryMatch.isPresent()) {
            return registryMatch.get();
        }

        // Strategy 3: Normalize to tokens, check alias map
        final String[] tokens = cleaned.split("\\W+");

        // Check single-token aliases first (e.g. "dijkstra", "dp", "bfs")
        for (final String token : tokens) {
            if (CONCEPT_ALIASES.containsKey(token)) {
                return "concept-" + CONCEPT_ALIASES.get(token);
            }
        }

        // Check two-token compound aliases (e.g. "dynamic programming", "sliding window")
        for (int i = 0; i < tokens.length - 1; i++) {
            final String bigram = tokens[i] + "-" + tokens[i + 1];
            if (CONCEPT_ALIASES.containsKey(bigram)) {
                return "concept-" + CONCEPT_ALIASES.get(bigram);
            }
        }

        // Strategy 4: fallback — strip stop words
        return "concept-" + Arrays.stream(cleaned.split("\\W+"))
                .filter(word -> !STOP_WORDS.contains(word))
                .filter(word -> !word.isBlank())
                .sorted()
                .collect(Collectors.joining("-"));
    }
}
