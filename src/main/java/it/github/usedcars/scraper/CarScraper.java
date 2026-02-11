package it.github.usedcars.scraper;

import it.github.usedcars.model.CarListing;
import it.github.usedcars.model.SearchCriteria;

import java.util.List;

public interface CarScraper {

    String getSourceName();

    List<CarListing> search(SearchCriteria criteria) throws ScraperException;

    String buildSearchUrl(SearchCriteria criteria);
}
