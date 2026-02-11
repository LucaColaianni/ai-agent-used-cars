package it.github.usedcars;

import it.github.usedcars.agent.AutoFinderAgent;
import it.github.usedcars.agent.AutoFinderAgentFactory;
import it.github.usedcars.config.AppConfig;
import it.github.usedcars.ui.ConsoleUI;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class AutoFinderApplication {

    public static void main(String[] args) {
        ConsoleUI ui = new ConsoleUI();

        try {
            ui.printWelcome();

            AppConfig config = AppConfig.load();
            AutoFinderAgent agent = AutoFinderAgentFactory.create(config);

            Terminal terminal = TerminalBuilder.builder().system(true).build();
            LineReader lineReader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();

            ui.printInstructions();

            while (true) {
                String userInput;
                try {
                    userInput = lineReader.readLine("\n[Tu] > ");
                } catch (UserInterruptException | EndOfFileException e) {
                    break;
                }

                if (userInput == null || userInput.trim().equalsIgnoreCase("exit")
                        || userInput.trim().equalsIgnoreCase("quit")) {
                    break;
                }

                if (userInput.trim().isEmpty()) {
                    continue;
                }

                try {
                    String response = agent.chat(userInput);
                    ui.printAgentResponse(response);
                } catch (Exception e) {
                    ui.printError("Errore nella risposta: " + e.getMessage());
                }
            }

            ui.printGoodbye();

        } catch (Exception e) {
            ui.printError("Errore fatale: " + e.getMessage());
            System.exit(1);
        }
    }
}
