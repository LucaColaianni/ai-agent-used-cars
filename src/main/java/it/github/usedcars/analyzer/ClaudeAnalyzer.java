package it.github.usedcars.analyzer;

import dev.langchain4j.model.chat.ChatLanguageModel;
import it.github.usedcars.model.AnalysisResult;
import it.github.usedcars.model.AnalysisResult.ListingAnalysis;
import it.github.usedcars.model.CarListing;
import it.github.usedcars.model.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClaudeAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(ClaudeAnalyzer.class);

    private final ChatLanguageModel chatModel;

    public ClaudeAnalyzer(ChatLanguageModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * Analizza qualitativamente i migliori annunci con Claude API.
     * Costruisce un prompt dettagliato e parsa la risposta strutturata.
     */
    public AnalysisResult analyzeListings(List<CarListing> topListings, UserProfile profile) {
        String prompt = buildAnalysisPrompt(topListings, profile);

        log.info("Invio richiesta analisi a Claude per {} annunci...", topListings.size());
        String responseText = chatModel.chat(prompt);
        log.debug("Risposta Claude ricevuta ({} caratteri)", responseText.length());

        return parseAnalysisResponse(responseText, topListings);
    }

    private String buildAnalysisPrompt(List<CarListing> listings, UserProfile profile) {
        StringBuilder sb = new StringBuilder();
        sb.append("Sei un esperto consulente di auto usate in Italia.\n");
        sb.append("L'utente cerca un'auto con queste caratteristiche:\n\n");
        sb.append(profile.toReadableSummary()).append("\n\n");
        sb.append("Ecco i ").append(listings.size()).append(" migliori annunci trovati:\n\n");

        for (int i = 0; i < listings.size(); i++) {
            sb.append("--- ANNUNCIO ").append(i + 1).append(" ---\n");
            sb.append(listings.get(i).toDetailedDescription()).append("\n\n");
        }

        sb.append("""
                Per ciascun annuncio, rispondi con ESATTAMENTE questo formato (una sezione per annuncio):

                ## ANNUNCIO 1
                GIUDIZIO: [giudizio sintetico in 1-2 frasi]
                PRO: [punto 1] | [punto 2] | [punto 3]
                CONTRO: [punto 1] | [punto 2] | [punto 3]
                RED FLAG: [eventuali segnali d'allarme, oppure "Nessuna"]
                VOTO: [numero da 1 a 10]

                ## ANNUNCIO 2
                ...e cosi via per ogni annuncio.

                Alla fine, aggiungi:

                ## TOP 3
                1. Annuncio [numero]: [breve motivazione]
                2. Annuncio [numero]: [breve motivazione]
                3. Annuncio [numero]: [breve motivazione]

                ## RIASSUNTO
                [2-3 frasi di riassunto generale con consigli per l'utente]

                Rispondi in italiano. Sii diretto e pratico.
                """);

        return sb.toString();
    }

    /**
     * Parsa la risposta di Claude nel formato strutturato AnalysisResult.
     * Il parsing e' tollerante: se il formato non e' perfetto, raccoglie quello che puo.
     */
    private AnalysisResult parseAnalysisResponse(String responseText, List<CarListing> listings) {
        List<ListingAnalysis> analyses = new ArrayList<>();

        // Parsa ogni sezione "## ANNUNCIO N"
        String[] sections = responseText.split("##\\s*ANNUNCIO\\s+\\d+");

        for (int i = 1; i < sections.length && i <= listings.size(); i++) {
            String section = sections[i];
            ListingAnalysis analysis = new ListingAnalysis();
            analysis.setListing(listings.get(i - 1));
            analysis.setJudgement(extractField(section, "GIUDIZIO"));
            analysis.setPros(extractListField(section, "PRO"));
            analysis.setCons(extractListField(section, "CONTRO"));
            analysis.setRedFlags(extractListField(section, "RED FLAG"));
            analysis.setMatchScore(extractScore(section));
            analyses.add(analysis);
        }

        // Se il parsing delle sezioni non ha funzionato, crea analisi minimali
        if (analyses.isEmpty()) {
            log.warn("Parsing strutturato fallito, creo analisi dalla risposta raw");
            for (CarListing listing : listings) {
                ListingAnalysis analysis = new ListingAnalysis();
                analysis.setListing(listing);
                analysis.setJudgement("Vedi analisi completa sotto");
                analysis.setPros(List.of());
                analysis.setCons(List.of());
                analysis.setRedFlags(List.of());
                analysis.setMatchScore(5);
                analyses.add(analysis);
            }
        }

        // Parsa TOP 3
        List<CarListing> topThree = parseTopThree(responseText, listings);

        // Parsa riassunto
        String summary = extractSummary(responseText);

        log.info("Analisi parsata: {} annunci analizzati, {} top picks", analyses.size(), topThree.size());
        return new AnalysisResult(analyses, topThree, summary);
    }

    private String extractField(String section, String fieldName) {
        Pattern pattern = Pattern.compile(fieldName + ":\\s*(.+?)(?:\n|$)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(section);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private List<String> extractListField(String section, String fieldName) {
        String raw = extractField(section, fieldName);
        if (raw.isEmpty() || raw.equalsIgnoreCase("nessuna") || raw.equalsIgnoreCase("nessuno")) {
            return List.of();
        }
        List<String> items = new ArrayList<>();
        for (String item : raw.split("\\|")) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                items.add(trimmed);
            }
        }
        return items;
    }

    private int extractScore(String section) {
        Pattern pattern = Pattern.compile("VOTO:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(section);
        if (matcher.find()) {
            int score = Integer.parseInt(matcher.group(1));
            return Math.max(1, Math.min(10, score));
        }
        return 5;
    }

    private List<CarListing> parseTopThree(String responseText, List<CarListing> listings) {
        List<CarListing> topThree = new ArrayList<>();
        Pattern pattern = Pattern.compile("##\\s*TOP\\s*3(.*?)(?:##|$)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(responseText);

        if (matcher.find()) {
            String topSection = matcher.group(1);
            Pattern numPattern = Pattern.compile("\\d+\\.\\s*Annuncio\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
            Matcher numMatcher = numPattern.matcher(topSection);
            while (numMatcher.find()) {
                int idx = Integer.parseInt(numMatcher.group(1)) - 1;
                if (idx >= 0 && idx < listings.size()) {
                    topThree.add(listings.get(idx));
                }
            }
        }

        // Fallback: se non ha trovato top 3, prendi i primi 3
        if (topThree.isEmpty() && listings.size() >= 3) {
            topThree.addAll(listings.subList(0, 3));
        } else if (topThree.isEmpty()) {
            topThree.addAll(listings);
        }

        return topThree;
    }

    private String extractSummary(String responseText) {
        Pattern pattern = Pattern.compile("##\\s*RIASSUNTO\\s*\n(.*?)$", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(responseText);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "Analisi completata. Consulta i dettagli per ogni annuncio.";
    }
}