package it.github.usedcars.ui;

public class ConsoleUI {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BOLD = "\u001B[1m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_CYAN = "\u001B[36m";

    public void printWelcome() {
        System.out.println();
        System.out.println(ANSI_BOLD + ANSI_CYAN + "╔══════════════════════════════════════════╗" + ANSI_RESET);
        System.out.println(ANSI_BOLD + ANSI_CYAN + "║          AUTO FINDER - v1.0               ║" + ANSI_RESET);
        System.out.println(ANSI_BOLD + ANSI_CYAN + "║   Il tuo consulente di auto usate AI     ║" + ANSI_RESET);
        System.out.println(ANSI_BOLD + ANSI_CYAN + "╚══════════════════════════════════════════╝" + ANSI_RESET);
        System.out.println();
    }

    public void printInstructions() {
        System.out.println(ANSI_YELLOW + "Dimmi cosa cerchi e ti aiutero a trovare l'auto perfetta!" + ANSI_RESET);
        System.out.println(ANSI_YELLOW + "Scrivi 'exit' o 'quit' per uscire." + ANSI_RESET);
        System.out.println();
    }

    public void printAgentResponse(String response) {
        System.out.println();
        System.out.println(ANSI_GREEN + "[AutoFinder]" + ANSI_RESET + " " + response);
    }

    public void printProgress(String message) {
        System.out.println(ANSI_CYAN + "[...] " + message + ANSI_RESET);
    }

    public void printWarning(String message) {
        System.out.println(ANSI_YELLOW + "[!] " + message + ANSI_RESET);
    }

    public void printError(String message) {
        System.out.println("\u001B[31m[ERRORE] " + message + ANSI_RESET);
    }

    public void printGoodbye() {
        System.out.println();
        System.out.println(ANSI_CYAN + "Grazie per aver usato AutoFinder! Buona ricerca!" + ANSI_RESET);
    }

    public void printSeparator() {
        System.out.println("──────────────────────────────────────────");
    }
}
