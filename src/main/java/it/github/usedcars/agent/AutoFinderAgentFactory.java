package it.github.usedcars.agent;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.service.AiServices;
import it.github.usedcars.analyzer.CarAnalyzer;
import it.github.usedcars.analyzer.ClaudeAnalyzer;
import it.github.usedcars.config.AppConfig;
import it.github.usedcars.scraper.CarScraper;
import it.github.usedcars.scraper.SubitoItScraper;
import it.github.usedcars.ui.ConsoleUI;
import it.github.usedcars.ui.ResultsPresenter;

import java.util.List;

public class AutoFinderAgentFactory {

    public static AutoFinderAgent create(AppConfig config) {
        AnthropicChatModel chatModel = AnthropicChatModel.builder()
                .apiKey(config.getAnthropicApiKey())
                .modelName("claude-sonnet-4-5-20250929")
                .temperature(0.7)
                .maxTokens(4096)
                .build();

        ConsoleUI ui = new ConsoleUI();

        List<CarScraper> scrapers = List.of(
                new SubitoItScraper(config.getMaxResultsPerSource())
        );

        CarAnalyzer carAnalyzer = new CarAnalyzer();
        ClaudeAnalyzer claudeAnalyzer = new ClaudeAnalyzer(chatModel);
        ResultsPresenter resultsPresenter = new ResultsPresenter();

        AgentTools tools = new AgentTools(scrapers, carAnalyzer, claudeAnalyzer,
                resultsPresenter, ui, config.getScraperDelayMs());

        return AiServices.builder(AutoFinderAgent.class)
                .chatLanguageModel(chatModel)
                .tools(tools)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                .build();
    }
}
