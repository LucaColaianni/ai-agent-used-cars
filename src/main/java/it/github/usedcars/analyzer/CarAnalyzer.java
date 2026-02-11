package it.github.usedcars.analyzer;

import it.github.usedcars.model.CarListing;
import it.github.usedcars.model.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

public class CarAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(CarAnalyzer.class);

    /**
     * Rimuove duplicati probabili: stesso titolo normalizzato + prezzo simile (±5%) + stessa zona.
     */
    public List<CarListing> deduplicate(List<CarListing> listings) {
        List<CarListing> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (CarListing listing : listings) {
            String key = buildDeduplicationKey(listing);
            if (seen.add(key)) {
                result.add(listing);
            } else {
                log.debug("Duplicato rimosso: {}", listing.toShortDescription());
            }
        }

        log.info("Deduplicazione: {} -> {} annunci", listings.size(), result.size());
        return result;
    }

    /**
     * Filtra annunci che rispettano i criteri base dell'utente.
     * Tolleranza del 10% sul budget per non escludere offerte al limite.
     */
    public List<CarListing> filterByCriteria(List<CarListing> listings, UserProfile profile) {
        int budgetLimit = (int) (profile.getMaxBudget() * 1.10);

        List<CarListing> filtered = listings.stream()
                .filter(l -> l.getPrice() > 0 && l.getPrice() <= budgetLimit)
                .filter(l -> profile.getMaxKilometers() <= 0 || l.getKilometers() <= 0
                        || l.getKilometers() <= profile.getMaxKilometers())
                .filter(l -> profile.getMinYear() <= 0 || l.getYear() <= 0
                        || l.getYear() >= profile.getMinYear())
                .collect(Collectors.toList());

        log.info("Filtro criteri: {} -> {} annunci", listings.size(), filtered.size());
        return filtered;
    }

    /**
     * Calcola uno score per ogni annuncio e ordina dal migliore al peggiore.
     * Lo score va da 0 a 100.
     */
    public List<CarListing> scoreAndRank(List<CarListing> listings, UserProfile profile) {
        for (CarListing listing : listings) {
            double score = calculateScore(listing, profile);
            listing.setScore(score);
        }

        List<CarListing> ranked = new ArrayList<>(listings);
        ranked.sort(Comparator.comparingDouble(CarListing::getScore).reversed());

        log.info("Ranking completato per {} annunci", ranked.size());
        return ranked;
    }

    /**
     * Restituisce i migliori N risultati.
     */
    public List<CarListing> getTopResults(List<CarListing> ranked, int limit) {
        return ranked.stream().limit(limit).collect(Collectors.toList());
    }

    // ---- Logica interna ----

    private String buildDeduplicationKey(CarListing listing) {
        String normalizedTitle = Optional.ofNullable(listing.getTitle())
                .orElse("")
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");

        // Arrotonda il prezzo al migliaio piu vicino per catturare duplicati con prezzo leggermente diverso
        int priceRounded = (listing.getPrice() / 1000) * 1000;

        String zone = Optional.ofNullable(listing.getLocation())
                .orElse("")
                .toLowerCase()
                .trim();

        return normalizedTitle + "|" + priceRounded + "|" + listing.getYear() + "|" + zone;
    }

    /**
     * Calcola lo score composito (0-100) basato su:
     * - Rapporto prezzo/budget (25 punti): piu basso = meglio
     * - Chilometri (20 punti): meno km = meglio, proporzionato all'eta
     * - Anno (20 punti): piu recente = meglio
     * - Match con priorita utente (20 punti)
     * - Completezza annuncio (15 punti): piu informazioni = piu affidabile
     */
    private double calculateScore(CarListing listing, UserProfile profile) {
        double priceScore = calculatePriceScore(listing, profile);
        double kmScore = calculateKmScore(listing);
        double yearScore = calculateYearScore(listing);
        double priorityScore = calculatePriorityScore(listing, profile);
        double completenessScore = calculateCompletenessScore(listing);

        return priceScore + kmScore + yearScore + priorityScore + completenessScore;
    }

    /** Prezzo: piu basso rispetto al budget = meglio (max 25 punti) */
    private double calculatePriceScore(CarListing listing, UserProfile profile) {
        if (listing.getPrice() <= 0 || profile.getMaxBudget() <= 0) return 12.5;
        double ratio = (double) listing.getPrice() / profile.getMaxBudget();
        if (ratio <= 0.5) return 25.0;
        if (ratio >= 1.1) return 0.0;
        // Scala lineare da 25 (ratio=0.5) a 0 (ratio=1.1)
        return 25.0 * (1.1 - ratio) / 0.6;
    }

    /** Km: meno km = meglio, normalizzato rispetto all'eta (max 20 punti) */
    private double calculateKmScore(CarListing listing) {
        if (listing.getKilometers() <= 0) return 10.0;

        int currentYear = Year.now().getValue();
        int age = Math.max(1, currentYear - listing.getYear());
        // Km medi annui attesi: ~15.000 km/anno
        double expectedKm = age * 15000.0;
        double ratio = listing.getKilometers() / expectedKm;

        if (ratio <= 0.5) return 20.0;
        if (ratio >= 2.0) return 0.0;
        return 20.0 * (2.0 - ratio) / 1.5;
    }

    /** Anno: piu recente = meglio (max 20 punti) */
    private double calculateYearScore(CarListing listing) {
        if (listing.getYear() <= 0) return 10.0;

        int currentYear = Year.now().getValue();
        int age = currentYear - listing.getYear();

        if (age <= 1) return 20.0;
        if (age >= 15) return 0.0;
        return 20.0 * (15 - age) / 14.0;
    }

    /**
     * Match con la priorita dell'utente (max 20 punti).
     * Euristica basata su correlazioni tipiche tipo veicolo / alimentazione / priorita.
     */
    private double calculatePriorityScore(CarListing listing, UserProfile profile) {
        String priority = Optional.ofNullable(profile.getPriority()).orElse("").toLowerCase();
        double score = 10.0; // valore neutro

        switch (priority) {
            case "prezzo basso":
                // Gia premiato dal price score, bonus per prezzo molto basso
                if (listing.getPrice() > 0 && profile.getMaxBudget() > 0) {
                    double ratio = (double) listing.getPrice() / profile.getMaxBudget();
                    if (ratio < 0.6) score = 20.0;
                    else if (ratio < 0.8) score = 15.0;
                }
                break;
            case "affidabilita":
                // Meno km e anno recente = piu affidabile
                if (listing.getKilometers() > 0 && listing.getKilometers() < 80000) score += 5;
                if (listing.getYear() > 0 && listing.getYear() >= Year.now().getValue() - 5) score += 5;
                score = Math.min(score, 20.0);
                break;
            case "consumi bassi":
                String fuel = Optional.ofNullable(listing.getFuelType()).orElse("").toLowerCase();
                if (fuel.contains("ibrida") || fuel.contains("elettrica") || fuel.contains("hybrid")) score = 20.0;
                else if (fuel.contains("diesel") || fuel.contains("gpl") || fuel.contains("metano")) score = 15.0;
                else if (fuel.contains("benzina")) score = 8.0;
                break;
            case "spazio":
                // Premiamo SUV, station wagon, monovolume
                String title = Optional.ofNullable(listing.getTitle()).orElse("").toLowerCase();
                if (title.contains("suv") || title.contains("station") || title.contains("wagon")
                        || title.contains("monovolume") || title.contains("spaziosa")) score = 18.0;
                break;
            case "prestazioni":
                // Se c'e la potenza, premiamo valori alti
                String power = Optional.ofNullable(listing.getPower()).orElse("");
                try {
                    int cv = Integer.parseInt(power.replaceAll("[^0-9]", ""));
                    if (cv >= 200) score = 20.0;
                    else if (cv >= 150) score = 16.0;
                    else if (cv >= 100) score = 12.0;
                } catch (NumberFormatException ignored) {}
                break;
            default:
                break;
        }

        return score;
    }

    /** Completezza: piu campi compilati = annuncio piu affidabile (max 15 punti) */
    private double calculateCompletenessScore(CarListing listing) {
        int fields = 0;
        if (listing.getTitle() != null && !listing.getTitle().isBlank()) fields++;
        if (listing.getPrice() > 0) fields++;
        if (listing.getYear() > 0) fields++;
        if (listing.getKilometers() > 0) fields++;
        if (listing.getFuelType() != null && !listing.getFuelType().isBlank()) fields++;
        if (listing.getTransmission() != null && !listing.getTransmission().isBlank()) fields++;
        if (listing.getPower() != null && !listing.getPower().isBlank()) fields++;
        if (listing.getLocation() != null && !listing.getLocation().isBlank()) fields++;
        if (listing.getImageUrl() != null && !listing.getImageUrl().isBlank()) fields++;
        // 9 campi totali
        return (fields / 9.0) * 15.0;
    }
}