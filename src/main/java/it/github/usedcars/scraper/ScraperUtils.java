package it.github.usedcars.scraper;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Random;

public class ScraperUtils {

    private static final Logger log = LoggerFactory.getLogger(ScraperUtils.class);
    private static final Random random = new Random();

    private static final List<String> USER_AGENTS = List.of(
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0"
    );

    public static Document fetchPage(String url) throws IOException {
        String userAgent = USER_AGENTS.get(random.nextInt(USER_AGENTS.size()));
        log.debug("Fetching URL: {}", url);

        Connection connection = Jsoup.connect(url)
                .userAgent(userAgent)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "it-IT,it;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Connection", "keep-alive")
                .timeout(15000)
                .followRedirects(true);

        return connection.get();
    }

    public static void delay(int baseMs) {
        try {
            // Delay randomizzato: base +/- 50%
            int jitter = random.nextInt(baseMs);
            int totalDelay = baseMs / 2 + jitter;
            Thread.sleep(totalDelay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static int parsePrice(String priceText) {
        if (priceText == null || priceText.isBlank()) return 0;
        // Rimuovi tutto tranne le cifre: "€ 12.500" -> "12500", "15.000 EUR" -> "15000"
        String cleaned = priceText.replaceAll("[^0-9]", "");
        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static int parseKilometers(String kmText) {
        if (kmText == null || kmText.isBlank()) return 0;
        // "125.000 km" -> "125000"
        String cleaned = kmText.replaceAll("[^0-9]", "");
        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static int parseYear(String yearText) {
        if (yearText == null || yearText.isBlank()) return 0;
        // Cerca un anno a 4 cifre nel testo: "01/2020" -> 2020, "2019" -> 2019
        var matcher = java.util.regex.Pattern.compile("(20\\d{2}|19\\d{2})").matcher(yearText);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }
}
