package cardcollector;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        System.out.println("Collecting cards from Wizards' Gatherer");
        CardParser parser = new CardParser(true);
        
        for(int multiverseId = 1; multiverseId < 400000; multiverseId++){
            System.out.println("parsing multiverse id = "+multiverseId);
            parser.processCard(multiverseId);
        }
    }
}
