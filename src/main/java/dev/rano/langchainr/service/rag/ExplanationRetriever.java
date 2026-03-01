package dev.rano.langchainr.service.rag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ExplanationRetriever {
    private static final double MIN_SCORE = 0.75;
    private static final int MAX_RESULTS = 3;

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public ExplanationRetriever(final EmbeddingModel embeddingModel,
                                final EmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    public Optional<String> retrieve(final String query) {
        final Embedding queryEmbedding = this.embeddingModel.embed(query).content();

        final EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(MAX_RESULTS)
                .minScore(MIN_SCORE)
                .build();

        final List<EmbeddingMatch<TextSegment>> matches = this.embeddingStore.search(request).matches();
        if (matches.isEmpty()) {
            return Optional.empty();
        }

        return matches.stream()
                .map(match -> match.embedded().text())
                .collect(Collectors.collectingAndThen(
                        Collectors.joining("\n\n---\n\n"),
                        Optional::of));
    }
}
