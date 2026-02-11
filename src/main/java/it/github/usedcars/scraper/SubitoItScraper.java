package it.github.usedcars.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.github.usedcars.model.CarListing;
import it.github.usedcars.model.SearchCriteria;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SubitoItScraper implements CarScraper {

    private static final Logger logger = LoggerFactory.getLogger(SubitoItScraper.class);
    private static final String SOURCE_NAME = "Subito.it";
    private static final String BASE_URL = "https://www.subito.it/annunci-italia/vendita/auto";
    private static final int REQUEST_DELAY_MS = 1000;

    private final int maxResults;
    private final ObjectMapper objectMapper;

    public SubitoItScraper(int maxResults) {
        this.maxResults = maxResults;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getSourceName() {
        return SOURCE_NAME;
    }

    @Override
    public String buildSearchUrl(SearchCriteria criteria) {
        return buildUrl(criteria, 1);
    }

    @Override
    public List<CarListing> search(SearchCriteria criteria) throws ScraperException {
        List<CarListing> allListings = new ArrayList<>();
        int maxPages = (maxResults / 20) + 1;

        for (int page = 1; page <= maxPages && allListings.size() < maxResults; page++) {
            logger.info("Scraping pagina {} di {}", page, maxPages);

            try {
                String url = buildUrl(criteria, page);
                logger.debug("URL: {}", url);

                List<CarListing> pageListings = scrapePage(url);

                if (pageListings.isEmpty()) {
                    logger.info("Nessun annuncio trovato a pagina {}, interruzione", page);
                    break;
                }

                allListings.addAll(pageListings);
                logger.info("Trovati {} annunci a pagina {}", pageListings.size(), page);

                if (page < maxPages) {
                    Thread.sleep(REQUEST_DELAY_MS);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ScraperException("Scraping interrotto", e);
            }
        }

        logger.info("Totale annunci trovati: {}", allListings.size());
        return allListings;
    }

    private String buildUrl(SearchCriteria criteria, int page) {
        StringBuilder urlBuilder = new StringBuilder(BASE_URL);

        if (criteria.getBrand() != null) {
            urlBuilder.append("/").append(criteria.getBrand().toLowerCase());
        }
        if (criteria.getModel() != null) {
            urlBuilder.append("/").append(criteria.getModel().toLowerCase().replace(" ", "-"));
        }
        if (criteria.getVehicleType() != null) {
            urlBuilder.append("/").append(criteria.getVehicleType().toLowerCase());
        }
        if (criteria.getFuelType() != null) {
            urlBuilder.append("/").append(criteria.getFuelType().toLowerCase());
        }

        urlBuilder.append("/?");

        List<String> params = new ArrayList<>();

        params.add("o=" + page);

        if (criteria.getMaxPrice() > 0) {
            params.add("pe=" + criteria.getMaxPrice());
        }

        urlBuilder.append(String.join("&", params));
        return urlBuilder.toString();
    }

    private List<CarListing> scrapePage(String url) throws ScraperException {
        List<CarListing> listings = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept-Language", "it-IT,it;q=0.9,en-US;q=0.8,en;q=0.7")
                    .timeout(30000)
                    .get();

            String jsonData = extractJsonData(doc);

            if (jsonData != null) {
                listings = parseJsonListings(jsonData);
            } else {
                logger.warn("Nessun dato JSON trovato nella pagina");
            }

        } catch (IOException e) {
            throw new ScraperException("Errore durante il download della pagina: " + url, e);
        }

        return listings;
    }

    private String extractJsonData(Document doc) {
        Elements scripts = doc.select("script");

        for (Element script : scripts) {
            String scriptContent = script.html();

            if (scriptContent.contains("\"ads\"") && scriptContent.contains("\"list\"")) {
                Pattern pattern = Pattern.compile("\\{\"ads\":\\{.*?\"list\":\\[.*?\\].*?\\}\\}", Pattern.DOTALL);
                Matcher matcher = pattern.matcher(scriptContent);

                if (matcher.find()) {
                    return matcher.group();
                }
            }

            if (scriptContent.contains("window.__NEXT_DATA__")) {
                Pattern pattern = Pattern.compile("window\\.__NEXT_DATA__\\s*=\\s*(\\{.*?\\});?\\s*(?:</script>|$)", Pattern.DOTALL);
                Matcher matcher = pattern.matcher(scriptContent);

                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        }

        Element nextDataScript = doc.selectFirst("script#__NEXT_DATA__");
        if (nextDataScript != null) {
            return nextDataScript.html();
        }

        return null;
    }

    private List<CarListing> parseJsonListings(String jsonData) throws ScraperException {
        List<CarListing> listings = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(jsonData);
            JsonNode adsNode = findAdsNode(root);

            if (adsNode != null && adsNode.isArray()) {
                for (JsonNode adNode : adsNode) {
                    try {
                        JsonNode itemNode = adNode.has("item") ? adNode.get("item") : adNode;

                        if (itemNode.has("kind") && !"AdItem".equals(itemNode.get("kind").asText())) {
                            continue;
                        }

                        CarListing listing = parseAdNode(itemNode);
                        if (listing != null) {
                            listings.add(listing);
                        }
                    } catch (Exception e) {
                        logger.warn("Errore parsing annuncio singolo: {}", e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            throw new ScraperException("Errore parsing JSON: " + e.getMessage(), e);
        }

        return listings;
    }

    private JsonNode findAdsNode(JsonNode root) {
        if (root.has("props")) {
            JsonNode props = root.get("props");
            if (props.has("pageProps")) {
                JsonNode pageProps = props.get("pageProps");
                if (pageProps.has("initialState")) {
                    JsonNode initialState = pageProps.get("initialState");
                    if (initialState.has("items")) {
                        JsonNode items = initialState.get("items");
                        if (items.has("list")) {
                            return items.get("list");
                        }
                    }
                }
            }
        }

        if (root.has("ads")) {
            JsonNode ads = root.get("ads");
            if (ads.has("list")) {
                return ads.get("list");
            }
        }

        return null;
    }

    private CarListing parseAdNode(JsonNode node) {
        CarListing listing = new CarListing();
        listing.setSource(SOURCE_NAME);

        if (node.has("subject")) {
            listing.setTitle(node.get("subject").asText());
        }

        if (node.has("urls") && node.get("urls").has("default")) {
            listing.setUrl(node.get("urls").get("default").asText());
        }

        if (node.has("geo")) {
            JsonNode geo = node.get("geo");
            StringBuilder location = new StringBuilder();

            if (geo.has("town") && geo.get("town").has("value")) {
                location.append(geo.get("town").get("value").asText());
            }
            if (geo.has("city") && geo.get("city").has("value")) {
                if (location.length() > 0) location.append(", ");
                location.append(geo.get("city").get("value").asText());
            }

            listing.setLocation(location.toString());
        }

        if (node.has("features")) {
            JsonNode features = node.get("features");

            Integer price = extractFeatureIntValue(features, "/price");
            if (price != null) listing.setPrice(price);

            Integer mileage = extractFeatureIntValue(features, "/mileage_scalar");
            if (mileage != null) listing.setKilometers(mileage);

            Integer year = extractFeatureIntValue(features, "/year");
            if (year != null) listing.setYear(year);

            String fuel = extractFeatureStringValue(features, "/fuel");
            if (fuel != null) listing.setFuelType(fuel);

            String gearbox = extractFeatureStringValue(features, "/gearbox");
            if (gearbox != null) listing.setTransmission(gearbox);

            String power = extractFeatureStringValue(features, "/power");
            if (power != null) listing.setPower(power);
        }

        return listing;
    }

    private Integer extractFeatureIntValue(JsonNode features, String key) {
        if (features.has(key)) {
            JsonNode feature = features.get(key);
            if (feature.has("values") && feature.get("values").isArray() && feature.get("values").size() > 0) {
                JsonNode firstValue = feature.get("values").get(0);
                if (firstValue.has("key")) {
                    try {
                        return Integer.parseInt(firstValue.get("key").asText());
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    private String extractFeatureStringValue(JsonNode features, String key) {
        if (features.has(key)) {
            JsonNode feature = features.get(key);
            if (feature.has("values") && feature.get("values").isArray() && feature.get("values").size() > 0) {
                JsonNode firstValue = feature.get("values").get(0);
                if (firstValue.has("value")) {
                    return firstValue.get("value").asText();
                }
            }
        }
        return null;
    }
}