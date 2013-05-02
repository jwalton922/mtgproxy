package com.mycompany.mtgcarddatacollector;

import cardcollector.CardParser;
import java.util.Map;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Unit test for simple App.
 */
public class AppTest
        extends TestCase {

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest(String testName) {
        super(testName);
        Logger log = Logger.getLogger(CardParser.class);
        System.out.println("Setting log level");
        log.setLevel(Level.TRACE);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(AppTest.class);
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp() {
        assertTrue(true);
    }

    public void testHybridMana() {
        int figureOfDestiny = 236456;
        CardParser parser = new CardParser(false);
        try {
            Map<String, Object> cardInfo = parser.getCardFromGatherer(figureOfDestiny);
            System.out.println("Card info = " + cardInfo.toString());
            Map mana = (Map) cardInfo.get(CardParser.MANA);
            int count = (Integer) mana.get("RED_OR_WHITE");
            assertTrue(count == 1);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }
}
