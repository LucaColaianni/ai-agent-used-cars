package it.github.usedcars.ui;

import it.github.usedcars.model.AnalysisResult;
import it.github.usedcars.model.AnalysisResult.ListingAnalysis;
import it.github.usedcars.model.CarListing;

import java.util.List;

public class ResultsPresenter {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BOLD = "\u001B[1m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_RED = "\u001B[31m";

    /**
     * Formatta i risultati ranked con l'analisi qualitativa per la visualizzazione console.
     */
    public String formatResults(List<CarListing> rankedResults, AnalysisResult analysisResult) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n");
        sb.append(ANSI_BOLD).append(ANSI_CYAN);
        sb.append("╔══════════════════════════════════════════════════════════╗\n");
        sb.append("║              RISULTATI DELLA RICERCA                    ║\n");
        sb.append("╚══════════════════════════════════════════════════════════╝\n");
        sb.append(ANSI_RESET);

        // Top 3 consigliati
        if (analysisResult != null && analysisResult.getTopThree() != null
                && !analysisResult.getTopThree().isEmpty()) {
            sb.append("\n");
            sb.append(ANSI_BOLD).append(ANSI_GREEN);
            sb.append("★ TOP 3 CONSIGLIATI ★\n");
            sb.append(ANSI_RESET);
            sb.append("──────────────────────────────────────────\n");
            int rank = 1;
            for (CarListing top : analysisResult.getTopThree()) {
                sb.append(ANSI_GREEN).append(rank).append(". ")
                        .append(top.getTitle()).append(ANSI_RESET)
                        .append(" - ").append(formatPrice(top.getPrice()))
                        .append(" | ").append(top.getSource()).append("\n");
                rank++;
            }
            sb.append("──────────────────────────────────────────\n");
        }

        // Dettaglio di ogni annuncio
        sb.append("\n").append(ANSI_BOLD).append("DETTAGLIO ANNUNCI:\n").append(ANSI_RESET);

        for (int i = 0; i < rankedResults.size(); i++) {
            CarListing listing = rankedResults.get(i);
            ListingAnalysis analysis = findAnalysis(analysisResult, listing);

            sb.append("\n");
            sb.append(ANSI_BOLD).append(ANSI_CYAN);
            sb.append("┌─ #").append(i + 1);
            sb.append(" ────────────────────────────────────────\n");
            sb.append(ANSI_RESET);

            // Info principali
            sb.append(ANSI_BOLD).append("  ").append(listing.getTitle()).append(ANSI_RESET).append("\n");
            sb.append("  Prezzo: ").append(ANSI_GREEN).append(formatPrice(listing.getPrice())).append(ANSI_RESET);
            sb.append(" | Anno: ").append(listing.getYear());
            sb.append(" | Km: ").append(formatKm(listing.getKilometers())).append("\n");

            sb.append("  Alimentazione: ").append(valueOrNd(listing.getFuelType()));
            sb.append(" | Cambio: ").append(valueOrNd(listing.getTransmission()));
            if (listing.getPower() != null && !listing.getPower().isBlank()) {
                sb.append(" | Potenza: ").append(listing.getPower());
            }
            sb.append("\n");

            sb.append("  Zona: ").append(valueOrNd(listing.getLocation()));
            sb.append(" | Fonte: ").append(listing.getSource()).append("\n");
            sb.append("  Link: ").append(listing.getUrl()).append("\n");
            sb.append("  Score: ").append(String.format("%.1f", listing.getScore())).append("/100\n");

            // Analisi qualitativa
            if (analysis != null) {
                sb.append("\n");
                if (!analysis.getJudgement().isEmpty()) {
                    sb.append("  ").append(ANSI_BOLD).append("Giudizio: ").append(ANSI_RESET)
                            .append(analysis.getJudgement()).append("\n");
                }
                sb.append("  ").append(ANSI_BOLD).append("Voto match: ").append(ANSI_RESET);
                sb.append(formatMatchScore(analysis.getMatchScore())).append("/10\n");

                if (!analysis.getPros().isEmpty()) {
                    sb.append("  ").append(ANSI_GREEN).append("PRO:").append(ANSI_RESET);
                    for (String pro : analysis.getPros()) {
                        sb.append(" + ").append(pro);
                    }
                    sb.append("\n");
                }

                if (!analysis.getCons().isEmpty()) {
                    sb.append("  ").append(ANSI_YELLOW).append("CONTRO:").append(ANSI_RESET);
                    for (String con : analysis.getCons()) {
                        sb.append(" - ").append(con);
                    }
                    sb.append("\n");
                }

                if (!analysis.getRedFlags().isEmpty()) {
                    sb.append("  ").append(ANSI_RED).append("RED FLAG:").append(ANSI_RESET);
                    for (String flag : analysis.getRedFlags()) {
                        sb.append(" ⚠ ").append(flag);
                    }
                    sb.append("\n");
                }
            }

            sb.append(ANSI_CYAN).append("└──────────────────────────────────────────\n").append(ANSI_RESET);
        }

        // Riassunto generale
        if (analysisResult != null && analysisResult.getOverallSummary() != null
                && !analysisResult.getOverallSummary().isEmpty()) {
            sb.append("\n");
            sb.append(ANSI_BOLD).append(ANSI_CYAN);
            sb.append("══════════════════════════════════════════\n");
            sb.append("  RIASSUNTO\n");
            sb.append("══════════════════════════════════════════\n");
            sb.append(ANSI_RESET);
            sb.append(analysisResult.getOverallSummary()).append("\n");
        }

        sb.append("\n").append(ANSI_CYAN)
                .append("Trovati ").append(rankedResults.size()).append(" annunci. ")
                .append("Vuoi approfondire qualche annuncio o modificare i criteri?")
                .append(ANSI_RESET).append("\n");

        return sb.toString();
    }

    private ListingAnalysis findAnalysis(AnalysisResult result, CarListing listing) {
        if (result == null || result.getAnalyses() == null) return null;
        return result.getAnalyses().stream()
                .filter(a -> a.getListing() == listing)
                .findFirst()
                .orElse(null);
    }

    private String formatPrice(int price) {
        if (price <= 0) return "N/D";
        return String.format("%,d EUR", price).replace(',', '.');
    }

    private String formatKm(int km) {
        if (km <= 0) return "N/D";
        return String.format("%,d km", km).replace(',', '.');
    }

    private String valueOrNd(String value) {
        return (value != null && !value.isBlank()) ? value : "N/D";
    }

    private String formatMatchScore(int score) {
        if (score >= 8) return ANSI_GREEN + score + ANSI_RESET;
        if (score >= 5) return ANSI_YELLOW + score + ANSI_RESET;
        return ANSI_RED + score + ANSI_RESET;
    }
}