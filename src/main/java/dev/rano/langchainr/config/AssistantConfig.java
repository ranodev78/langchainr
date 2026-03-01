package dev.rano.langchainr.config;

import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.rano.langchainr.service.LeetCodeAssistant;
import dev.rano.langchainr.service.reviewer.SolutionReviewerAssistant;
import dev.rano.langchainr.service.reviewer.SolutionSynthesizerAssistant;
import dev.rano.langchainr.service.reviewer.StreamingSynthesizerAssistant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AssistantConfig {
    private final ChatLanguageModel chatLanguageModel;

    public AssistantConfig(final ChatLanguageModel chatLanguageModel) {
        this.chatLanguageModel = chatLanguageModel;
    }

    @Bean
    public LeetCodeAssistant leetCodeAssistant() {
        return AiServices.builder(LeetCodeAssistant.class)
                .chatLanguageModel(this.chatLanguageModel)
                .build();
    }

    @Bean
    public SolutionReviewerAssistant solutionReviewerAssistant() {
        return AiServices.builder(SolutionReviewerAssistant.class)
                .chatLanguageModel(this.chatLanguageModel)
                .build();
    }

    @Bean
    SolutionSynthesizerAssistant solutionSynthesizerAssistant() {
        return AiServices.builder(SolutionSynthesizerAssistant.class)
                .chatLanguageModel(this.chatLanguageModel)
                .build();
    }

    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel(
            @Value("${langchain4j.anthropic.chat-model.api-key}") final String apiKey,
            @Value("${langchain4j.anthropic.chat-model.model-name}") final String model) {
        return AnthropicStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .maxTokens(8096)
                .build();
    }

    @Bean
    public StreamingSynthesizerAssistant streamingSynthesizerAssistant(
            final StreamingChatLanguageModel streamingChatLanguageModel) {
        return AiServices.builder(StreamingSynthesizerAssistant.class)
                .streamingChatLanguageModel(streamingChatLanguageModel)
                .build();
    }
}
