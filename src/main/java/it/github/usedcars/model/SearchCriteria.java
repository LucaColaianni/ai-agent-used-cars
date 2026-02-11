package it.github.usedcars.model;

public class SearchCriteria {

    private int maxPrice;
    private int maxKilometers;
    private int minYear;
    private String fuelType;
    private String vehicleType;
    private String transmission;
    private String brand;
    private String model;
    private String location;

    public static SearchCriteria fromProfile(UserProfile profile) {
        SearchCriteria criteria = new SearchCriteria();
        criteria.maxPrice = profile.getMaxBudget();
        criteria.maxKilometers = profile.getMaxKilometers();
        criteria.minYear = profile.getMinYear();
        criteria.fuelType = normalizeOrNull(profile.getFuelType(), "qualsiasi");
        criteria.vehicleType = normalizeOrNull(profile.getVehicleType(), "non ho preferenze");
        criteria.transmission = normalizeOrNull(profile.getTransmission(), "indifferente");
        criteria.location = profile.getLocation();

        // Separa marca e modello se presenti (es. "Volkswagen Golf" -> marca=Volkswagen, modello=Golf)
        String preferred = profile.getPreferredBrand();
        if (preferred != null && !preferred.isBlank()) {
            String[] parts = preferred.trim().split("\\s+", 2);
            criteria.brand = parts[0];
            criteria.model = parts.length > 1 ? parts[1] : null;
        }

        return criteria;
    }

    private static String normalizeOrNull(String value, String ignoreValue) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase(ignoreValue)) {
            return null;
        }
        return value.toLowerCase().trim();
    }

    // Getters
    public int getMaxPrice() { return maxPrice; }
    public int getMaxKilometers() { return maxKilometers; }
    public int getMinYear() { return minYear; }
    public String getFuelType() { return fuelType; }
    public String getVehicleType() { return vehicleType; }
    public String getTransmission() { return transmission; }
    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public String getLocation() { return location; }
}