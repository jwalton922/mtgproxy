package cardcollector;

import java.io.IOException;

/**
 * Hello world!
 *
 */
public class App {

    public static void main(String[] args) {
        System.out.println("Collecting cards from Wizards' Gatherer");
        CardParser parser = new CardParser(true);

        for (int multiverseId = 7826; multiverseId < 400000; multiverseId++) {
            long sleep = 1000L;
            System.out.println("parsing multiverse id = " + multiverseId);
            try {
                parser.processCard(multiverseId);
            } catch (IOException e) {
                System.out.println("Sleeping " + sleep + " ms and retrying");
                try {
                    Thread.sleep(sleep);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                System.out.println("Woke up from: " + sleep);
                sleep *= 2;

            }
        }
    }
}
