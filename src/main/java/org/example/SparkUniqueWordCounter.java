package org.example;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.example.utils.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This class is responsible for executing the Apache SparK program to calculate the
 * unique words frequency in the Reuters News file.
 */
public class SparkUniqueWordCounter implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(SparkUniqueWordCounter.class);

    private static final long serialVersionUID = 2397200127855301363L;

    /**
     * creates the SparkConf with the app name and other configurations set.
     *
     * @return the SparkConf with the app name and additional configurations
     */
    public static SparkConf getSparkConfig() {
        return new SparkConf().setAppName("reutersFrequencyCounter").setMaster("yarn");
    }

    public static void main(String[] args) {
        logger.info("**** Running the spark program to get the frequency count of unique words ****.");
        JavaSparkContext sparkContext = new JavaSparkContext(getSparkConfig());

        JavaRDD<String> reutersNewsContentRDD = sparkContext.textFile(ConfigUtils.GCP_FILE_NAME_FOR_WORD_COUNT);
        JavaRDD<String> reutersNewsTextRDD = reutersNewsContentRDD.flatMap(new ReutersTextExtractor());

        JavaPairRDD<String, Integer> wordCounts = reutersNewsTextRDD
                .mapToPair(word -> new Tuple2<>(word, 1))
                .reduceByKey(Integer::sum);

        // Sort words by frequency in descending order
        JavaPairRDD<String, Integer> sortedWordCounts = wordCounts
                .mapToPair(Tuple2::swap) // Swap key-value pairs to use sortByKey
                .sortByKey(false) // Sort in descending order
                .mapToPair(Tuple2::swap); // Swap back to original order

        // Print sorted word counts
        //sortedWordCounts.collect().forEach(System.out::println);

        sortedWordCounts.coalesce(1).saveAsTextFile(ConfigUtils.GCP_FREQUENCY_COUNT_FILE);


    }

    /**
     * Static class that implements the FlatMapFunction interface for extracting and processing
     * news data from Reuters articles.
     */
    private static class ReutersTextExtractor implements FlatMapFunction<String, String> {

        StringBuilder eachReutersNewsBuffer = new StringBuilder();
        StringBuilder extractedPreCleanedBuffer = new StringBuilder();

        /**
         * Removes specified punctuation symbols, parentheses, and digits from the given content.
         *
         * @param contentExtracted the input content from which symbols and digits should be removed
         * @return a new string with the specified symbols, parentheses, and digits removed
         */
        public static String removeStopSymbolsAndDigits(String contentExtracted) {
            contentExtracted = contentExtracted.replace(",", " ");
            contentExtracted = contentExtracted.replace(".", " ");
            contentExtracted = contentExtracted.replace(";", " ");
            contentExtracted = contentExtracted.replace(":", " ");
            contentExtracted = contentExtracted.replace("\"", "");
            contentExtracted = contentExtracted.replace("(", " ");
            contentExtracted = contentExtracted.replace(")", " ");
            contentExtracted = contentExtracted.replace("/", " ");
            contentExtracted = contentExtracted.replace("-", " ");
            // Removed commas and periods, hyphen and numbers, colons etc
            String sanitizedContent = contentExtracted.replaceAll("\\d", "");
            return sanitizedContent.trim();
        }

        @Override
        public Iterator<String> call(String line) {
            List<String> tokens = new LinkedList<>();
            //System.out.println("***INSIDE CALL METHOD LINE: " + line);
            eachReutersNewsBuffer.append(line).append(ConfigUtils.LINE_SEPARATOR);

            Pattern reutersNewsPattern = Pattern.compile("<REUTERS.*?>(.*?)</REUTERS>", Pattern.DOTALL);
            Pattern titlePattern = Pattern.compile("<TITLE>(.*?)</TITLE>", Pattern.DOTALL);
            Pattern bodyPattern = Pattern.compile("<BODY>(.*?)</BODY>", Pattern.DOTALL);
            Pattern dateLinePattern = Pattern.compile("<DATELINE>(.*?)</DATELINE>", Pattern.DOTALL);
            Pattern authorPattern = Pattern.compile("<AUTHOR>(.*?)</AUTHOR>", Pattern.DOTALL);

            Matcher textArticleMatcher = reutersNewsPattern.matcher(eachReutersNewsBuffer);

            // executes when reuters news block is found
            if (textArticleMatcher.find()) {
                String textArticleBlock = textArticleMatcher.group(1);

                // Extract and append content for each section
                extractAndAppendContent(textArticleBlock, titlePattern);
                extractAndAppendContent(textArticleBlock, bodyPattern);
                extractAndAppendContent(textArticleBlock, dateLinePattern);
                extractAndAppendContent(textArticleBlock, authorPattern);
                tokens.addAll(sanitizeInputData(extractedPreCleanedBuffer.toString().toLowerCase()));

                // reset the interim buffer for next reuters news article
                eachReutersNewsBuffer = new StringBuilder();
                extractedPreCleanedBuffer = new StringBuilder();
            }

            return tokens.iterator(); // Return an empty iterable since we are printing to the console
        }

        /**
         * Extracts content based on the provided pattern and appends it to the pre-cleaned buffer.
         *
         * @param textArticleBlock The text block to extract content from.
         * @param pattern          The pattern used for content extraction.
         */
        private void extractAndAppendContent(String textArticleBlock, Pattern pattern) {
            Matcher matcher = pattern.matcher(textArticleBlock);
            if (matcher.find()) {
                String content = matcher.group(1);
                extractedPreCleanedBuffer.append(content).append(ConfigUtils.LINE_SEPARATOR);
            }
        }

        /**
         * @param extractedReutersNews lowercase news line,which is used for data cleaning
         * @return final sanitized tokens which are free from stop words and invalid characters
         * @implNote cleans the input by removing unwanted characters and removes stop words
         */
        private List<String> sanitizeInputData(String extractedReutersNews) {
            String sanitizedNews = ConfigUtils.getSanitizedContent(extractedReutersNews);
            String stopSymbolsAndDigitsRemovedContent = removeStopSymbolsAndDigits(sanitizedNews);
            List<String> finalSanitizedLineTokens = new ArrayList<>(Arrays.asList(stopSymbolsAndDigitsRemovedContent.split("\\s+")));
            finalSanitizedLineTokens.removeAll(Arrays.asList(ConfigUtils.STOP_WORDS));
            return finalSanitizedLineTokens;
        }

    }


}
