package it.github.usedcars.model;

import java.util.List;

public class AnalysisResult {

    private List<ListingAnalysis> analyses;
    private List<CarListing> topThree;
    private String overallSummary;

    public AnalysisResult() {}

    public AnalysisResult(List<ListingAnalysis> analyses, List<CarListing> topThree, String overallSummary) {
        this.analyses = analyses;
        this.topThree = topThree;
        this.overallSummary = overallSummary;
    }

    public List<ListingAnalysis> getAnalyses() { return analyses; }
    public void setAnalyses(List<ListingAnalysis> analyses) { this.analyses = analyses; }

    public List<CarListing> getTopThree() { return topThree; }
    public void setTopThree(List<CarListing> topThree) { this.topThree = topThree; }

    public String getOverallSummary() { return overallSummary; }
    public void setOverallSummary(String overallSummary) { this.overallSummary = overallSummary; }

    public static class ListingAnalysis {
        private CarListing listing;
        private String judgement;
        private List<String> pros;
        private List<String> cons;
        private List<String> redFlags;
        private int matchScore; // 1-10

        public ListingAnalysis() {}

        public CarListing getListing() { return listing; }
        public void setListing(CarListing listing) { this.listing = listing; }

        public String getJudgement() { return judgement; }
        public void setJudgement(String judgement) { this.judgement = judgement; }

        public List<String> getPros() { return pros; }
        public void setPros(List<String> pros) { this.pros = pros; }

        public List<String> getCons() { return cons; }
        public void setCons(List<String> cons) { this.cons = cons; }

        public List<String> getRedFlags() { return redFlags; }
        public void setRedFlags(List<String> redFlags) { this.redFlags = redFlags; }

        public int getMatchScore() { return matchScore; }
        public void setMatchScore(int matchScore) { this.matchScore = matchScore; }
    }
}
