package it.github.usedcars.scraper;

public class ScraperException extends RuntimeException {

    public ScraperException(String message) {
        super(message);
    }

    public ScraperException(String message, Throwable cause) {
        super(message, cause);
    }
}
