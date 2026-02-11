package it.github.usedcars.agent;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import it.github.usedcars.analyzer.CarAnalyzer;
import it.github.usedcars.analyzer.ClaudeAnalyzer;
import it.github.usedcars.model.*;
import it.github.usedcars.scraper.CarScraper;
import it.github.usedcars.scraper.ScraperUtils;
import it.github.usedcars.ui.ConsoleUI;
import it.github.usedcars.ui.ResultsPresenter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AgentTools {

    private static final Logger log = LoggerFactory.getLogger(AgentTools.class);

    private final List<CarScraper> scrapers;
    private final CarAnalyzer carAnalyzer;
    private final ClaudeAnalyzer claudeAnalyzer;
    private final ResultsPresenter resultsPresenter;
    private final ConsoleUI ui;
    private final int scraperDelayMs;

    // Stato della sessione
    private UserProfile userProfile;
    private List<CarListing> searchResults;
    private List<CarListing> rankedResults;
    private AnalysisResult analysisResult;

    public AgentTools(List<CarScraper> scrapers, CarAnalyzer carAnalyzer,
                      ClaudeAnalyzer claudeAnalyzer, ResultsPresenter resultsPresenter,
                      ConsoleUI ui, int scraperDelayMs) {
        this.scrapers = scrapers;
        this.carAnalyzer = carAnalyzer;
        this.claudeAnalyzer = claudeAnalyzer;
        this.resultsPresenter = resultsPresenter;
        this.ui = ui;
        this.scraperDelayMs = scraperDelayMs;
    }

    @Tool("Salva il profilo utente con le preferenze raccolte. Chiama questo tool quando hai raccolto tutte le preferenze dell'utente.")
    public String saveUserProfile(
            @P("Budget massimo in euro") int maxBudget,
            @P("Uso principale: citta, extraurbano, autostrada, misto") String mainUsage,
            @P("Tipo veicolo: utilitaria, berlina, SUV, station wagon, monovolume, sportiva") String vehicleType,
            @P("Alimentazione: benzina, diesel, GPL, metano, ibrida, elettrica, qualsiasi") String fuelType,
            @P("Cambio: manuale, automatico, indifferente") String transmission,
            @P("Chilometri massimi accettati, 0 per qualsiasi") int maxKilometers,
            @P("Anno minimo di immatricolazione, 0 per qualsiasi") int minYear,
            @P("Marca e/o modello preferito, vuoto se nessuna preferenza") String preferredBrand,
            @P("Zona geografica: provincia o regione") String location,
            @P("Priorita principale: affidabilita, consumi bassi, spazio, prestazioni, prezzo basso") String priority) {

        this.userProfile = new UserProfile(maxBudget, mainUsage, vehicleType, fuelType,
                transmission, maxKilometers, minYear, preferredBrand, location, priority);

        log.info("Profilo utente salvato: budget={}, zona={}", maxBudget, location);
        return "Profilo salvato con successo. Ora puoi mostrare il riepilogo con showProfileSummary.";
    }

    @Tool("Mostra il riepilogo del profilo utente. Chiama dopo saveUserProfile per far confermare le preferenze all'utente.")
    public String showProfileSummary() {
        if (userProfile == null) {
            return "Errore: nessun profilo salvato. Usa prima saveUserProfile.";
        }
        return userProfile.toReadableSummary() + "\n\nChiedi all'utente se vuole confermare o modificare qualcosa.";
    }

    @Tool("Avvia la ricerca di annunci auto su AutoScout24, Subito.it e Automobile.it. Chiama solo dopo che l'utente ha confermato il profilo.")
    public String startCarSearch() {
        if (userProfile == null) {
            return "Errore: devi prima salvare il profilo con saveUserProfile.";
        }

        SearchCriteria criteria = SearchCriteria.fromProfile(userProfile);
        searchResults = new ArrayList<>();

        for (CarScraper scraper : scrapers) {
            try {
                ui.printProgress("Cerco su " + scraper.getSourceName() + "...");
                List<CarListing> results = scraper.search(criteria);
                searchResults.addAll(results);
                ui.printProgress(scraper.getSourceName() + ": trovati " + results.size() + " annunci");
            } catch (Exception e) {
                log.warn("Errore scraping {}: {}", scraper.getSourceName(), e.getMessage());
                ui.printWarning("Impossibile cercare su " + scraper.getSourceName() + ": " + e.getMessage());
            }

            ScraperUtils.delay(scraperDelayMs);
        }

        if (searchResults.isEmpty()) {
            return "Nessun annuncio trovato con i criteri specificati. Suggerisci all'utente di allargare i criteri di ricerca.";
        }

        Map<String, Long> bySource = searchResults.stream()
                .collect(Collectors.groupingBy(CarListing::getSource, Collectors.counting()));

        StringBuilder sb = new StringBuilder();
        sb.append("Ricerca completata! Trovati ").append(searchResults.size()).append(" annunci totali:\n");
        bySource.forEach((source, count) ->
                sb.append("- ").append(source).append(": ").append(count).append("\n"));
        sb.append("\nOra puoi procedere con analyzeAndRankResults per analizzare i risultati.");

        return sb.toString();
    }

    @Tool("Analizza e classifica i risultati della ricerca. Rimuove duplicati, calcola score e chiede a Claude una analisi qualitativa dei top 10.")
    public String analyzeAndRankResults() {
        if (searchResults == null || searchResults.isEmpty()) {
            return "Errore: nessun risultato di ricerca. Usa prima startCarSearch.";
        }

        ui.printProgress("Rimuovo duplicati e calcolo score...");

        // Analisi deterministica
        List<CarListing> deduplicated = carAnalyzer.deduplicate(searchResults);
        List<CarListing> filtered = carAnalyzer.filterByCriteria(deduplicated, userProfile);
        List<CarListing> scored = carAnalyzer.scoreAndRank(filtered, userProfile);
        rankedResults = carAnalyzer.getTopResults(scored, 10);

        ui.printProgress("Trovati " + rankedResults.size() + " risultati dopo deduplicazione e ranking.");

        if (rankedResults.isEmpty()) {
            return "Dopo la deduplicazione e il filtro, non ci sono risultati validi. Suggerisci di allargare i criteri.";
        }

        // Analisi qualitativa con Claude
        ui.printProgress("Chiedo a Claude un'analisi dettagliata dei migliori " + rankedResults.size() + " annunci...");
        try {
            analysisResult = claudeAnalyzer.analyzeListings(rankedResults, userProfile);
        } catch (Exception e) {
            log.error("Errore analisi Claude: {}", e.getMessage());
            return "Analisi deterministica completata con " + rankedResults.size() + " risultati, "
                    + "ma l'analisi AI non e' riuscita: " + e.getMessage()
                    + "\nPuoi comunque mostrare i risultati con presentResults.";
        }

        return "Analisi completata! " + rankedResults.size() + " annunci analizzati con score e analisi qualitativa. "
                + "Usa presentResults per mostrare i risultati all'utente.";
    }

    @Tool("Mostra i risultati finali con raccomandazioni, pro/contro e top 3. Chiama dopo analyzeAndRankResults.")
    public String presentResults() {
        if (rankedResults == null || rankedResults.isEmpty()) {
            return "Errore: nessun risultato analizzato. Usa prima analyzeAndRankResults.";
        }

        return resultsPresenter.formatResults(rankedResults, analysisResult);
    }

    // Accessors per testing
    UserProfile getUserProfile() { return userProfile; }
    List<CarListing> getSearchResults() { return searchResults; }
}
