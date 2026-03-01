package dev.rano.langchainr.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

@Component
public class LeetCodeProblemRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(LeetCodeProblemRegistry.class);

    private final Map<String, String> slugToKey = new HashMap<>();
    private final Map<String, String> titleToKey = new HashMap<>();

    public LeetCodeProblemRegistry() {
        try (InputStream inputStream = getClass().getResourceAsStream("/data/merged_problems.json")) {
            // Read as generic tree first to inspect the structure
            final ObjectMapper objectMapper = new ObjectMapper();
            final JsonNode root = objectMapper.readTree(inputStream);

            // Determine if root is an array or a wrapper object
            final JsonNode problemsNode = root.isArray() ? root : findArrayNode(root);

            if (problemsNode == null) {
                throw new RuntimeException("Could not find problems array in JSON");
            }

            for (final JsonNode problem : problemsNode) {
                final String id = problem.path("frontend_id").asText();
                final String slug = problem.path("title_slug").asText();   // check exact key name
                final String title = problem.path("title").asText();

                if (id.isBlank() || title.isBlank()) {
                    continue;
                }

                final String cacheKey = "lc-" + id;
                this.slugToKey.put(slug.toLowerCase(), cacheKey);
                this.titleToKey.put(title.toLowerCase().trim(), cacheKey);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load LeetCode problem registry", e);
        }

        LOGGER.info("LeetCode registry loaded: {} problems", titleToKey.size());
    }

    // Recursively find the first array node inside a wrapper object
    private static JsonNode findArrayNode(final JsonNode node) {
        final Iterator<JsonNode> fields = node.elements();
        while (fields.hasNext()) {
            final JsonNode child = fields.next();
            if (child.isArray()) {
                return child;
            }
        }

        return null;
    }

    public Optional<String> resolve(final String cleanedInput) {
        if (this.titleToKey.containsKey(cleanedInput)) {
            return Optional.of(this.titleToKey.get(cleanedInput));
        }

        if (this.slugToKey.containsKey(cleanedInput)) {
            return Optional.of(this.slugToKey.get(cleanedInput));
        }

        return this.titleToKey.entrySet().stream()
                .filter(entry -> cleanedInput.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();
    }
}
