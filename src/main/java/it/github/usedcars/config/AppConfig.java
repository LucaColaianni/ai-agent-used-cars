package it.github.usedcars.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    private final String anthropicApiKey;
    private final int maxResultsPerSource;
    private final int scraperDelayMs;

    private AppConfig(String anthropicApiKey, int maxResultsPerSource, int scraperDelayMs) {
        this.anthropicApiKey = anthropicApiKey;
        this.maxResultsPerSource = maxResultsPerSource;
        this.scraperDelayMs = scraperDelayMs;
    }

    public static AppConfig load() {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        String apiKey = dotenv.get("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("your_key_here")) {
            log.error("ANTHROPIC_API_KEY non configurata. Crea un file .env con la tua API key.");
            log.error("Copia .env.example in .env e inserisci la tua chiave da console.anthropic.com");
            System.exit(1);
        }

        int maxResults = parseIntOrDefault(dotenv.get("MAX_RESULTS_PER_SOURCE"), 30);
        int delay = parseIntOrDefault(dotenv.get("SCRAPER_DELAY_MS"), 2000);

        log.info("Configurazione caricata: maxResults={}, delay={}ms", maxResults, delay);
        return new AppConfig(apiKey, maxResults, delay);
    }

    private static int parseIntOrDefault(String value, int defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public String getAnthropicApiKey() { return anthropicApiKey; }
    public int getMaxResultsPerSource() { return maxResultsPerSource; }
    public int getScraperDelayMs() { return scraperDelayMs; }
}