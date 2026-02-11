package it.github.usedcars.model;

public class CarListing {

    private String title;
    private int price;
    private int year;
    private int kilometers;
    private String fuelType;
    private String transmission;
    private String power;          // CV/kW se disponibile
    private String location;
    private String url;
    private String source;         // AutoScout24, Subito.it, Automobile.it
    private String imageUrl;
    private double score;          // calcolato da CarAnalyzer

    public CarListing() {}

    public String toShortDescription() {
        return String.format("%s | %d EUR | %d km | %d | %s | %s",
                title, price, kilometers, year, fuelType, source);
    }

    public String toDetailedDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("Titolo: ").append(title).append("\n");
        sb.append("Prezzo: ").append(price).append(" EUR\n");
        sb.append("Anno: ").append(year).append("\n");
        sb.append("Km: ").append(kilometers).append("\n");
        sb.append("Alimentazione: ").append(fuelType).append("\n");
        sb.append("Cambio: ").append(transmission != null ? transmission : "N/D").append("\n");
        sb.append("Potenza: ").append(power != null ? power : "N/D").append("\n");
        sb.append("Localita: ").append(location != null ? location : "N/D").append("\n");
        sb.append("Fonte: ").append(source).append("\n");
        sb.append("Link: ").append(url);
        return sb.toString();
    }

    // Getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getKilometers() { return kilometers; }
    public void setKilometers(int kilometers) { this.kilometers = kilometers; }

    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }

    public String getTransmission() { return transmission; }
    public void setTransmission(String transmission) { this.transmission = transmission; }

    public String getPower() { return power; }
    public void setPower(String power) { this.power = power; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
}