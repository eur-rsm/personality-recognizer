package nl.rsm.scidev;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Interface to the LIWC dictionary, implementing patterns for each LIWC
 * category based on the LIWC.CAT file.
 *
 * @author Francois Mairesse, <a href=http://www.mairesse.co.uk
 *         target=_top>http://www.mairesse.co.uk</a>
 * @version 1.01
 */
public class LIWCDictionary
{
  /**
   * Mapping associating LIWC features to regular expression patterns.
   */
  private Map<String, Pattern> map;

  /**
   * Loads dictionary from LIWC dictionary tab-delimited text file (with
   * variable names as first row). Each word category is converted into a
   * regular expression that is a disjunction of all its members.
   *
   * @param catFile dictionary file, it should be pointing to the LIWC.CAT file of
   *                the Linguistic Inquiry and Word Count software (Pennebaker &
   *                Francis, 2001).
   */
  public LIWCDictionary (File catFile)
  {
    try {
      map = loadLIWCDictionary(catFile);
      System.err.println("LIWC dictionary loaded ("
          + map.size() + " lexical categories)");
    }
    catch (IOException e) {
      System.err.println("Error: file " + catFile + " doesn't exist");
      e.printStackTrace();
      System.exit(1);
    }
    catch (NullPointerException e) {
      System.err.println("Error: LIWC dicitonary file " + catFile +
          " doesn't have the right format");
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Loads dictionary from LIWC dictionary tab-delimited text file (with
   * variable names as first row). Each word category is converted into a
   * regular expression that is a disjunction of all its members.
   *
   * @param dicFile dictionary file, it should be pointing to the LIWC.CAT file
   *                of the Linguistic Inquiry and Word Count software
   *                (Pennebaker & Francis, 2001).
   * @return hashtable associating each category with a regular expression
   * (Pattern object) matching each word.
   */
  private Map<String, Pattern> loadLIWCDictionary (File dicFile)
      throws IOException
  {
    BufferedReader reader = new BufferedReader(new FileReader(dicFile));
    String line;

    Map<String, Pattern> wordLists = new LinkedHashMap<String, Pattern>();
    String currentVariable = "";
    String catRegex = "";
    int word_count = 0;

    while ((line = reader.readLine()) != null) {
      // if encounter new variable
      if (line.matches("\\t[\\w ]+")) {
        // add full regex to database
        if (!catRegex.equals("")) {
          catRegex = catRegex.substring(0, catRegex.length() - 1);
          catRegex = "(" + catRegex + ")";
          catRegex = catRegex.replaceAll("\\*", "[\\\\w']*");
          wordLists.put(currentVariable, Pattern.compile(catRegex));
        }
        // update variable
        currentVariable = line.split("\t")[1];
        catRegex = "";
      }
      else if (line.matches("\t\t.+ \\(\\d+\\)")) {
        word_count++;
        String newPattern = line.split("\\s+")[1].toLowerCase();
        catRegex += "\\b" + newPattern + "\\b|";
      }
    }
    //  add last regex to database
    if (!catRegex.equals("")) {
      catRegex = catRegex.substring(0, catRegex.length() - 1);
      catRegex = "(" + catRegex + ")";
      catRegex = catRegex.replaceAll("\\*", "[\\\\w']*");
      wordLists.put(currentVariable, Pattern.compile(catRegex));
    }

    reader.close();

    return wordLists;
  }

  /**
   * Returns a map associating each LIWC categories to the number of
   * their occurences in the input text. The counts are computed matching
   * patterns loaded. It doesn't produce punctuation counts.
   *
   * @param text           input text.
   * @param absoluteCounts includes counts that aren't relative to the total word
   *                       count (e.g. actual word count).
   * @return hashtable associating each LIWC category with the percentage of
   * words in the text belonging to it.
   */
  public Map<String, Double> getCounts (String text, boolean absoluteCounts)
  {

    Map<String, Double> counts = new LinkedHashMap<String, Double>(map.size());
    String[] words = tokenize(text);
    String[] sentences = splitSentences(text);
    double percFactor = 100.0 / words.length;

    System.err.println("Input text splitted into " + words.length
        + " words and " + sentences.length + " sentences");

    // word count (NOT A PROPER FEATURE)
    if (absoluteCounts) {
      counts.put("WC", (double) words.length);
    }
    counts.put("WPS", 1.0 * words.length / sentences.length);

    // type token ratio, words with more than 6 letters, abbreviations,
    // emoticons, numbers
    int sixletters = 0;
    int numbers = 0;
    for (String word : words) {
      if (word.length() > 6) {
        sixletters++;
      }
      if (word.matches("-?[,\\d+]*\\.?\\d+")) {
        numbers++;
      }
    }

    Set<String> types = new LinkedHashSet<String>(Arrays.asList(words));
    counts.put("UNIQUE", percFactor * types.size());
    counts.put("SIXLTR", percFactor * sixletters);
    // abbreviations
    counts.put("ABBREVIATIONS",
        percFactor * Utils.countMatches("\\w\\.(\\w\\.)+", text));
    // emoticons
    counts.put("EMOTICONS",
        percFactor * Utils.countMatches("[:;8%]-[\\)\\(\\@\\[\\]\\|]+", text));
    // text ending with a question mark
    counts.put("QMARKS",
        100.0 * Utils.countMatches("\\w\\s*\\?", text) / sentences.length);

    // punctuation
    int period = Utils.countMatches("\\.", text);
    counts.put("PERIOD", percFactor * period);
    int comma = Utils.countMatches(",", text);
    counts.put("COMMA", percFactor * comma);
    int colon = Utils.countMatches(":", text);
    counts.put("COLON", percFactor * colon);
    int semicolon = Utils.countMatches(";", text);
    counts.put("SEMIC", percFactor * semicolon);
    int qmark = Utils.countMatches("\\?", text);
    counts.put("QMARK", percFactor * qmark);
    int exclam = Utils.countMatches("!", text);
    counts.put("EXCLAM", percFactor * exclam);
    int dash = Utils.countMatches("-", text);
    counts.put("DASH", percFactor * dash);
    int quote = Utils.countMatches("\"", text);
    counts.put("QUOTE", percFactor * quote);
    int apostr = Utils.countMatches("'", text);
    counts.put("APOSTRO", percFactor * apostr);
    int parent = Utils.countMatches("[\\(\\[{]", text);
    counts.put("PARENTH", percFactor * parent);
    int otherp = Utils.countMatches("[^\\w\\d\\s\\.:;\\?!\"'\\(\\{\\[,-]", text);
    counts.put("OTHERP", percFactor * otherp);
    int allp = period + comma + colon + semicolon + qmark + exclam + dash
        + quote + apostr + parent + otherp;
    counts.put("ALLPCT", percFactor * allp);

    // PATTERN MATCHING

    // store word in dic
    Set<Integer> indic = new HashSet<Integer>();

    // first get all lexical counts
    for (String cat : map.keySet()) {
      // add entry to output hash
      Pattern catRegex = map.get(cat);

      int catCount = 0;
      for (int i = 0; i < words.length; i++) {
        String word = words[i].toLowerCase();
        Matcher m = catRegex.matcher(word);
        while (m.find()) {
          catCount++;
          indic.add(i);
        }
      }
      // Don't use percFactor: rounding errors
      counts.put(cat, 100.0 * catCount / words.length);
    }

    // put ratio of words matched
    counts.put("DIC", percFactor * indic.size());
    // add numerical numbers, don't use percFactor: rounding errors
    counts.put("NUMBERS", counts.get("NUMBERS") + 100.0 * numbers / words.length);

    return counts;
  }

  /**
   * Splits a text into words separated by non-word characters.
   *
   * @param text text to tokenize.
   * @return an array of words.
   */
  public static String[] tokenize (String text)
  {
    String words_only = text.replaceAll("\\W+\\s*", " ").replaceAll(
        "\\s+$", "").replaceAll("^\\s+", "");
    return words_only.split("\\s+");
  }

  /**
   * Splits a text into sentences separated by a dot, exclamation point or
   * question mark.
   *
   * @param text text to tokenize.
   * @return an array of sentences.
   */
  public static String[] splitSentences (String text)
  {
    return text.split("\\s*[\\.!\\?]+\\s+");
  }
}
