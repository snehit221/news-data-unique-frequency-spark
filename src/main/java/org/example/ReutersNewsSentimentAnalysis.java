package org.example;

import org.example.utils.ConfigUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

/**
 * Performs Sentiment Analysis using BOW model on title of Reuters News Articles Titles
 */
public class ReutersNewsSentimentAnalysis {


    private Set<String> positiveWords = new HashSet<>();

    private Set<String> negativeWords = new HashSet<>();

    private List<ReutersNewsSentiment> reutersNewsSentimentList = new LinkedList<>();


    public static void main(String[] args) {
        ReutersNewsSentimentAnalysis reutersNewsSentimentAnalysis = new ReutersNewsSentimentAnalysis();
        List<ReutersNews> reutersNewsList = getReutersNewsList();

        // populate the +ve words set
        reutersNewsSentimentAnalysis.populatePositiveWordsSet(ConfigUtils.POSITIVE_WORDS_FILE);
        // populate the -ve words set
        reutersNewsSentimentAnalysis.populateNegativeWordsSet(ConfigUtils.NEGATIVE_WORDS_FILE);

        System.out.println("Performing Sentiment Analysis on each Reuters News title \n");
        reutersNewsSentimentAnalysis.analyzeSentimentForEachTitle(reutersNewsList);

        // load each reuters news sentiment to the table
        reutersNewsSentimentAnalysis.loadReutersNewsSentimentListToDB();
    }

    /**
     * Retrieves a list of ReutersNews objects.
     * <p>
     * This method initializes a ReutRead object, which is responsible for reading Reuters news files from MongoDB.
     * It then returns a list of ReutersNews objects obtained from the specified data source.
     * </p>
     *
     * @return A List of ReutersNews objects representing the news data.
     * @see ReutRead
     * @see ReutersNews
     */
    private static List<ReutersNews> getReutersNewsList() {
        ReutRead reutRead = new ReutRead();

        // reutRead.processReutersNewsFiles();
        return reutRead.getAllReutersNewsFromMongoDB();

    }

    /**
     * Populates the polarity of a ReutersNewsSentiment object based on a sentiment score.
     * <p>
     * This method takes a ReutersNewsSentiment object and a sentiment score as parameters. It sets the polarity
     * of the ReutersNewsSentiment based on the sentiment score:
     * - If the sentiment score is greater than 0, the polarity is set to positive.
     * - If the sentiment score is less than 0, the polarity is set to negative.
     * - If the sentiment score is 0, the polarity is set to neutral.
     * </p>
     *
     * @param reutersNewsSentiment The ReutersNewsSentiment object to populate with polarity.
     * @param sentimentScore       The sentiment score used to determine the polarity.
     * @see ReutersNewsSentiment
     * @see ConfigUtils
     */
    private static void populateNewsTitlePolarity(ReutersNewsSentiment reutersNewsSentiment, int sentimentScore) {
        if (sentimentScore > 0) {
            reutersNewsSentiment.setPolarity(ConfigUtils.POSITIVE);
        } else if (sentimentScore < 0) {
            reutersNewsSentiment.setPolarity(ConfigUtils.NEGATIVE);
        } else {
            // case when it is 0
            reutersNewsSentiment.setPolarity(ConfigUtils.NEUTRAL);
        }
    }

    /**
     * Establishes a connection with the MySQL database and loads the list of {@link ReutersNewsSentiment}
     * to the table named "ReutersNewsSentiment".
     */
    private void loadReutersNewsSentimentListToDB() {

        Connection localDbConnection = null;
        try {
            localDbConnection = DriverManager.getConnection(ConfigUtils.LOCAL_MYSQL_DB_URL, ConfigUtils.LOCAL_DB_USER, ConfigUtils.LOCAL_DB_PASSWORD);

            System.out.println("Going to insert the each reutersNewsSentiment to the table...");
            int tupleCount = 0;
            for (ReutersNewsSentiment reutersNewsSentiment : reutersNewsSentimentList) {
                String insertOrderSql = "INSERT INTO ReutersNewsSentiment (newsTitleContent, wordMatch, score, polarity) VALUES (?, ?, ?, ?)";
                PreparedStatement insertReutersNewsSentiment = localDbConnection.prepareStatement(insertOrderSql);
                insertReutersNewsSentiment.setString(1, reutersNewsSentiment.getNewsTitleContent());
                insertReutersNewsSentiment.setString(2, reutersNewsSentiment.getWordMatch());
                insertReutersNewsSentiment.setInt(3, reutersNewsSentiment.getScore());
                insertReutersNewsSentiment.setString(4, reutersNewsSentiment.getPolarity());
                insertReutersNewsSentiment.executeUpdate();
                tupleCount++;
            }
            System.out.println("SUCCESS: Inserted records: " + tupleCount);
        } catch (SQLException e) {
            System.out.println("ERROR while populating the entries to the database" + e);
        }

    }

    /**
     * performs sentiment analysis for each Reuters new title
     *
     * @param reutersNewsList reuters news, which contains the title to be analyzed
     * @return ReutersNewsSentiment tuple, which is stored in the sql table later
     */
    private List<ReutersNewsSentiment> analyzeSentimentForEachTitle(List<ReutersNews> reutersNewsList) {

//        System.out.println("+ve word size " + positiveWords.size());
//        System.out.println("-ve word size " + negativeWords.size());
        for (ReutersNews reutersNews : reutersNewsList) {
            ReutersNewsSentiment reutersNewsSentiment = new ReutersNewsSentiment();
            StringJoiner wordMatch = new StringJoiner(",");

            String newsTitle = reutersNews.getTitle();
            int sentimentScore = calculateWordMatchWithSentimentScore(tokenizeNewsTitle(newsTitle), wordMatch);

            reutersNewsSentiment.setNewsTitleContent(newsTitle);
            reutersNewsSentiment.setWordMatch(wordMatch.toString());
            reutersNewsSentiment.setScore(sentimentScore);
            populateNewsTitlePolarity(reutersNewsSentiment, sentimentScore);

            reutersNewsSentimentList.add(reutersNewsSentiment);

        }
        return reutersNewsSentimentList;
    }

    /**
     * calculates the sentiment score of the given news title and populates the wordMatch
     * for every positive and negative word, the counter is incremented and decremented respectively
     * if the score is > 0 , it means overall positive sentiment,
     * else if it is < 0 , it means the sentiment is negative
     * else the score is neutral.
     *
     * @param tokenizeNewsTitle tokens of the Reuters news title
     * @param wordMatch
     * @return final sentiment score
     */
    private int calculateWordMatchWithSentimentScore(Map<String, Integer> tokenizeNewsTitle, StringJoiner wordMatch) {
        int positiveWordScore = 0;
        int negativeWordScore = 0;

        for (String keyWord : tokenizeNewsTitle.keySet()) {
            // if the word is +ve, add the +ve score
            if (positiveWords.contains(keyWord)) {
                positiveWordScore++;
                wordMatch.add(keyWord);
            }
            if (negativeWords.contains(keyWord)) {
                negativeWordScore--;
                wordMatch.add(keyWord);
            }

        }
        return positiveWordScore + negativeWordScore;
    }

    /**
     * Tokenizes a given news title into a bag-of-words representation.
     *
     * @param newsTitle The input news title to be tokenized.
     * @return A Map representing the bag-of-words for the given news title, where keys are individual
     * tokens and values are the corresponding token frequencies.
     */
    private Map<String, Integer> tokenizeNewsTitle(String newsTitle) {
        String[] titleTokens = newsTitle.toLowerCase().split("\\s+");
        Map<String, Integer> titleBagOfWords = new HashMap<>();
        for (String titleToken : titleTokens) {
            titleBagOfWords.put(titleToken, titleBagOfWords.getOrDefault(titleToken, 0) + 1);
        }
        return titleBagOfWords;
    }

    /**
     * @param positiveWordsFilePath
     * @return the set of unique positive words
     */
    private Set<String> populatePositiveWordsSet(String positiveWordsFilePath) {
        return populateWordSet(positiveWordsFilePath, this.positiveWords);
    }

    /**
     * @param negativeWordsFilePath
     * @return the set of unique negative words
     */
    private Set<String> populateNegativeWordsSet(String negativeWordsFilePath) {
        return populateWordSet(negativeWordsFilePath, this.negativeWords);
    }

    /**
     * Populates a Set with words from a specified file path.
     *
     * @param wordsFilePath The path to the file containing words to be added to the Set.
     * @param wordSet       The Set to be populated with words from the file.
     * @return The populated Set containing unique, lowercase words from the file.
     * @throws IOException If an error occurs while reading the word file.
     */
    private Set<String> populateWordSet(String wordsFilePath, Set<String> wordSet) {

        try (BufferedReader reader = new BufferedReader(new FileReader(wordsFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                wordSet.add(line.trim().toLowerCase());
            }
        } catch (IOException e) {
            System.out.println("Unable to read word file" + e);
        }
        return wordSet;
    }
}
