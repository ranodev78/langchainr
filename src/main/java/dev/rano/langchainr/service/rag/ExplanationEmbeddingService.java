package dev.rano.langchainr.service.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.springframework.stereotype.Service;

@Service
public class ExplanationEmbeddingService {
    private final EmbeddingStoreIngestor ingestor;

    public ExplanationEmbeddingService(final EmbeddingModel embeddingModel,
                                       final EmbeddingStore<TextSegment> embeddingStore) {
        this.ingestor = EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .documentSplitter(DocumentSplitters.recursive(512, 64))
                .build();
    }

    public void embed(final String title, final String markdownContent) {
        final Metadata metadata = Metadata.from("title", title);
        final Document document = Document.from(markdownContent, metadata);
        this.ingestor.ingest(document);
    }
}
