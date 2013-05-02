/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cardcollector;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author Josh
 */
public class CardImageCollector {

    public final static String SITE_ROOT = "http://magiccards.info";
    private Set<String> processedCards = new HashSet<String>();

    public void getImages() {

        try {
            Document setListDoc = Jsoup.connect(SITE_ROOT + "/sitemap.html").get();
            Elements links = setListDoc.select("li a");
            Set<String> processedLinks = new HashSet<String>();
            for (Element link : links) {
                String href = link.attr("href");
                if (href.indexOf("en.html") > 0) {
                    if (processedLinks.contains(href)) {
                        System.out.println("Skipping link: " + href + ". Alread processed");
                    } else {
                        System.out.println("Processing set: " + href);
                        processSet(href);
                        processedLinks.add(href);
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processSet(String setLink) {
        String setString = "";
        String[] setLinkSplit = setLink.split("/");
        for (int i = 0; i < setLinkSplit.length; i++) {
            String part = setLinkSplit[i];
            if (part.length() > 0) {
                setString = part;
                break;
            }
        }
        try {
            Document setDoc = Jsoup.connect(SITE_ROOT + setLink).get();
            Elements tables = setDoc.select("table");
            //System.out.println("Found " + tables.size() + " tables");
            for (Element table : tables) {
                Elements headers = table.select("th");
                boolean isRightTable = false;
                for (Element header : headers) {
                    if (header.text().equalsIgnoreCase("card name")) {
                        isRightTable = true;
                        break;
                    }
                }

                if (isRightTable) {
                    // System.out.println("Found table to process!");
                    Elements cardLinks = table.select("tr td a");
                    for (Element cardLink : cardLinks) {
                        String cardName = cardLink.text().trim().toUpperCase();
                        String cardSource = cardLink.attr("href");
                        if (!processedCards.contains(cardName)) {
                            String fileName = cardName.replaceAll(" ", "_");
                            String[] cardSourceSplit = cardSource.split("/");
                            cardSource = cardSourceSplit[cardSourceSplit.length -1];
                            cardSource = cardSource.replaceAll("html", "jpg");
                            URL imageSource = new URL(SITE_ROOT + "/scans/en/" + setString+"/"+cardSource);
                            File imageFile = new File("C:/Users/Josh/mtgImages/" + fileName + ".jpg");
                            System.out.println("Writing " + imageSource + " to file: " + imageFile);
                            FileUtils.copyURLToFile(imageSource, imageFile);
                            processedCards.add(cardName);
                            
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        CardImageCollector collector = new CardImageCollector();
        collector.getImages();
    }
}
