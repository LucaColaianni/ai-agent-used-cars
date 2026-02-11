package it.github.usedcars.model;

public class UserProfile {

    private int maxBudget;
    private String mainUsage;        // citta, extraurbano, autostrada, misto
    private String vehicleType;      // utilitaria, berlina, SUV, station wagon, monovolume, sportiva
    private String fuelType;         // benzina, diesel, GPL, metano, ibrida, elettrica, qualsiasi
    private String transmission;     // manuale, automatico, indifferente
    private int maxKilometers;
    private int minYear;
    private String preferredBrand;   // campo libero, opzionale
    private String location;         // provincia o regione
    private String priority;         // affidabilita, consumi bassi, spazio, prestazioni, prezzo basso

    public UserProfile() {}

    public UserProfile(int maxBudget, String mainUsage, String vehicleType, String fuelType,
                       String transmission, int maxKilometers, int minYear, String preferredBrand,
                       String location, String priority) {
        this.maxBudget = maxBudget;
        this.mainUsage = mainUsage;
        this.vehicleType = vehicleType;
        this.fuelType = fuelType;
        this.transmission = transmission;
        this.maxKilometers = maxKilometers;
        this.minYear = minYear;
        this.preferredBrand = preferredBrand;
        this.location = location;
        this.priority = priority;
    }

    public String toReadableSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== RIEPILOGO PREFERENZE ===\n");
        sb.append("Budget massimo: ").append(maxBudget).append(" EUR\n");
        sb.append("Uso principale: ").append(mainUsage).append("\n");
        sb.append("Tipo veicolo: ").append(vehicleType).append("\n");
        sb.append("Alimentazione: ").append(fuelType).append("\n");
        sb.append("Cambio: ").append(transmission).append("\n");
        sb.append("Km massimi: ").append(maxKilometers > 0 ? maxKilometers : "qualsiasi").append("\n");
        sb.append("Anno minimo: ").append(minYear > 0 ? minYear : "qualsiasi").append("\n");
        sb.append("Marca/modello: ").append(preferredBrand != null && !preferredBrand.isBlank() ? preferredBrand : "nessuna preferenza").append("\n");
        sb.append("Zona: ").append(location).append("\n");
        sb.append("Priorita: ").append(priority).append("\n");
        sb.append("============================");
        return sb.toString();
    }

    // Getters and setters
    public int getMaxBudget() { return maxBudget; }
    public void setMaxBudget(int maxBudget) { this.maxBudget = maxBudget; }

    public String getMainUsage() { return mainUsage; }
    public void setMainUsage(String mainUsage) { this.mainUsage = mainUsage; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }

    public String getTransmission() { return transmission; }
    public void setTransmission(String transmission) { this.transmission = transmission; }

    public int getMaxKilometers() { return maxKilometers; }
    public void setMaxKilometers(int maxKilometers) { this.maxKilometers = maxKilometers; }

    public int getMinYear() { return minYear; }
    public void setMinYear(int minYear) { this.minYear = minYear; }

    public String getPreferredBrand() { return preferredBrand; }
    public void setPreferredBrand(String preferredBrand) { this.preferredBrand = preferredBrand; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
}