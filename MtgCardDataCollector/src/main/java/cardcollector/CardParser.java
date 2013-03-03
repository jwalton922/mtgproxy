/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cardcollector;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author jwalton
 */
public class CardParser {

    private final String MONGO_HOST = "ds033477.mongolab.com";
    private final int MONGO_PORT = 33477;
    private final String MONGO_DB = "mtg";
    private final String MONGO_COLLECTION = "cards";
    private DBCollection cardCollection;
    private final static Logger log = Logger.getLogger(CardParser.class);
    public static String GATHERER_ROOT = "http://gatherer.wizards.com/Pages/Card/Details.aspx?printed=true&multiverseid=";
    public final static String GATHERER_ELEMENT_ROOT = "#ctl00_ctl00_ctl00_MainContent_SubContent_SubContent_";
    public final static String NAME_ELEMENT = GATHERER_ELEMENT_ROOT + "nameRow div.value";
    public final static String MANA_ELEMENT = GATHERER_ELEMENT_ROOT + "manaRow div.value";
    public final static String CMC_ELEMENT = GATHERER_ELEMENT_ROOT + "cmcRow div.value";
    public final static String TYPE_ELEMENT = GATHERER_ELEMENT_ROOT + "typeRow div.value";
    public final static String TEXT_ELEMENT = GATHERER_ELEMENT_ROOT + "textRow div.value";
    public final static String PT_ELEMENT = GATHERER_ELEMENT_ROOT + "ptRow div.value";
    public final static String RARITY_ELEMENT = GATHERER_ELEMENT_ROOT + "rarityRow div.value";
    public final static String NAME = "name";
    public final static String MANA = "mana";
    public final static String CMC = "cmc";
    public final static String TYPES = "types";
    public final static String SUBTYPES = "subtypes";
    public final static String TEXT = "text";
    public final static String POWER = "power";
    public final static String TOUGHNESS = "toughness";
    public final static String RARITY = "rarity";
    private List<String> types = new ArrayList<String>();
    private List<String> keywords = new ArrayList<String>();
    private boolean saveCards = false;

    public CardParser(boolean saveCards) {
        BasicConfigurator.configure();
        init();
        this.saveCards = saveCards;
        if (this.saveCards) {
            try {
                Mongo m = new Mongo(MONGO_HOST, MONGO_PORT);
                DB db = m.getDB(MONGO_DB);
                cardCollection = db.getCollection(MONGO_COLLECTION);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    private void init() {
        types.add("CREATURE");
        types.add("SORCERY");
        types.add("INSTANT");
        types.add("ARTIFACT");
        types.add("ENCHANTMENT");
        types.add("LAND");
        types.add("PLANESWALKER");
        types.add("TRIBAL");
    }

    public Map<String, Object> getCardFromGatherer(int id) {
        Map<String, Object> cardInfo = null;
        try {
            Document doc = Jsoup.connect(GATHERER_ROOT + id).get();
            cardInfo = parseCard(doc);
        } catch (ParseException e) {
            log.error("Error parsing card: " + e.getLocalizedMessage());
        } catch (Exception e) {
            log.error("Error connecting to the gatherer: " + e.getLocalizedMessage());
        }

        return cardInfo;

    }

    private void saveCard(Map<String, Object> cardInfo) {
        DBObject object = new BasicDBObject(cardInfo);
        try {
            cardCollection.insert(object);
        } catch (Exception e) {
            log.error("Error inserting card: "+e.getLocalizedMessage());
        }
    }

    public void processCard(int id) {
        Map<String, Object> cardInfo = getCardFromGatherer(id);
        cardInfo.put("multiverseid", id);
        if (log.isTraceEnabled()) {
            log.trace("Card " + id + " = " + cardInfo.toString());
        }
        boolean validCard = validateCard(cardInfo, id);
        if (validCard) {
            if (saveCards) {
                saveCard(cardInfo);
            }
        } else {
            log.debug("Invalid card number: " + id);
        }
    }

    public Map<String, Object> parseCard(Document doc) throws ParseException {
        Map<String, Object> cardInfo = new HashMap<String, Object>();
        try {
            parseName(doc, cardInfo);
            parseMana(doc, cardInfo);
            parseCmc(doc, cardInfo);
            parseTypes(doc, cardInfo);
            parseText(doc, cardInfo);
            parsePowerAndToughnesss(doc, cardInfo);
            parseRarity(doc, cardInfo);
        } catch (Exception e) {
            log.error("Error parsing card");
            throw new ParseException(e);
        }

        return cardInfo;
    }

    private boolean validateCard(Map<String, Object> cardInfo, int id) {
        boolean valid = true;
        String errorMessage = "Invalid card for id = " + id;

        if (cardInfo.get(NAME) == null) {
            log.error(errorMessage + ", could not find name");
            return false;
        }

        String name = (String) cardInfo.get(NAME);
        if (name.length() <= 1) {
            log.error(errorMessage + ", invalid name: " + name);
            return false;
        }

        if (cardInfo.get(MANA) == null) {
            log.error(errorMessage + ", no mana associated with card");
            return false;
        }

        if (cardInfo.get(TYPES) == null) {
            log.error(errorMessage + ", no types associated with card");
            return false;
        }

        return valid;
    }

    private void parseRarity(Document doc, Map<String, Object> cardInfo) {
        Elements rarityElements = doc.select(RARITY_ELEMENT);
        for (Element rarityElement : rarityElements) {
            String rarity = rarityElement.text().trim().toUpperCase();
            cardInfo.put(RARITY, rarity);
        }
    }

    private void parsePowerAndToughnesss(Document doc, Map<String, Object> cardInfo) {
        Elements ptElements = doc.select(PT_ELEMENT);
        for (Element ptElement : ptElements) {
            String ptString = ptElement.text().trim();
            String[] ptSplit = ptString.split("/");
            String power = "";
            String toughness = "";
            if (ptSplit.length == 2) {
                power = ptSplit[0].trim();
                toughness = ptSplit[1].trim();
            } else {
                log.error("Unknown power and toughness string: " + ptString);
            }

            try {
                int powerNum = Integer.parseInt(power);
                cardInfo.put(POWER, powerNum);
            } catch (Exception e) {
                cardInfo.put(POWER, power);
            }

            try {
                int tNum = Integer.parseInt(toughness);
                cardInfo.put(TOUGHNESS, tNum);
            } catch (Exception e) {
                cardInfo.put(TOUGHNESS, toughness);
            }
        }
    }

    private void parseText(Document doc, Map<String, Object> cardInfo) {
        Elements textElements = doc.select(TEXT_ELEMENT);
        for (Element textElement : textElements) {
            String text = textElement.text().trim();
            cardInfo.put(TEXT, text);
        }
    }

    private void parseTypes(Document doc, Map<String, Object> cardInfo) {
        Elements typeElements = doc.select(TYPE_ELEMENT);
        List<String> cardTypes = new ArrayList<String>();
        List<String> cardSubTypes = new ArrayList<String>();
        for (Element typeElement : typeElements) {
            String typeString = typeElement.text().trim().toUpperCase();
            String[] typeStringSplit = typeString.split("\\s+");
            for (int i = 0; i < typeStringSplit.length; i++) {
                String type = typeStringSplit[i];
                if (type.length() > 1) {
                    if (types.contains(type)) {
                        cardTypes.add(type);
                    } else {
                        cardSubTypes.add(type);
                    }
                }
            }
        }

        if (types.size() > 0) {
            cardInfo.put(TYPES, cardTypes);
        }
        if (cardSubTypes.size() > 0) {
            cardInfo.put(SUBTYPES, cardSubTypes);
        }
    }

    private void parseCmc(Document doc, Map<String, Object> cardInfo) {
        Elements cmcElements = doc.select(CMC_ELEMENT);
        for (Element cmcElement : cmcElements) {
            String cmcString = cmcElement.text().trim();
            try {
                int cmc = Integer.parseInt(cmcString);
                cardInfo.put(CMC, cmc);
            } catch (Exception e) {
                //for X mana symbols
                cardInfo.put(CMC, cmcString);
            }
        }
    }

    private void parseMana(Document doc, Map<String, Object> cardInfo) {
        Map<String, Integer> manaInfo = new HashMap<String, Integer>();
        Elements manaImages = doc.select(MANA_ELEMENT + " img");
        log.trace("Found " + manaImages.size() + " mana image elements");
        //System.out.println("Found "+manaImages.size()+" mana image elements");
        for (Element manaImageElement : manaImages) {
            String manaString = manaImageElement.attr("alt").trim().toUpperCase();
            try {
                Integer colorless = Integer.parseInt(manaString);
                manaInfo.put("colorless", colorless);
            } catch (Exception e) {
                manaString = manaString.replace(" ", "_");
                if (manaInfo.get(manaString) == null) {
                    manaInfo.put(manaString, 1);
                } else {
                    Integer count = manaInfo.get(manaString);
                    count++;
                    manaInfo.put(manaString, count);
                }
            }

        }
        cardInfo.put(MANA, manaInfo);
    }

    private void parseName(Document doc, Map<String, Object> cardInfo) {
        Elements nameElements = doc.select(NAME_ELEMENT);
        for (Element nameElement : nameElements) {
            String name = nameElement.text().trim().toUpperCase();
            log.trace("Parsing card: " + name);
            cardInfo.put(NAME, name);
        }
    }

    private class ParseException extends Exception {

        public ParseException(String message) {
            super(message);
        }

        public ParseException(Exception e) {
            super(e);
        }
    }
}
