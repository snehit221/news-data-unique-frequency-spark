package org.example.utils;

public class ConfigUtils {
    public static final String MONGO_CONNECTION_URL = "mongodb+srv://snehit221:root1234@clusterdatalab.sgymx8h.mongodb.net/?retryWrites=true&w=majority";
    public static final String MONGO_DATABASE_NAME = "ReuterDb";
    public static final String MONGO_COLLECTION_NAME = "ReuterNews";
    public static final String LOCAL_MYSQL_DB_URL = "jdbc:mysql://localhost:3306/sentiment_analysis";
    public static final String LOCAL_DB_USER = "root";
    public static final String LOCAL_DB_PASSWORD = "root1234";


    public static final String START_BODY_TAG = "<BODY>";
    public static final String END_BODY_TAG = "</BODY>";
    public static final String LINE_SEPARATOR = "\n";
    public static final String REUTERS_END_TAG = "</REUTERS>";
    public static final String PIPE_DELIMITER = "|";
    public static final String REUTERS_NEWS_FILE1 = "src/main/resources/reut2-009.sgm";
    public static final String REUTERS_NEWS_FILE2 = "src/main/resources/reut2-014.sgm";
    public static final String GCP_FILE_NAME_FOR_WORD_COUNT = "reut2-009.sgm";

    public static final String GCP_FREQUENCY_COUNT_FILE = "unique_words_frequency.txt";


    // TODO
    public static final String TEST = "src/main/resources/test.sgm";


    public static final String[] STOP_WORDS = {"-", ",", ".", "i", "me", "my", "myself", "we", "our", "ours", "ourselves",
            "you", "your", "yours", "yourself", "yourselves", "he", "him", "his", "himself", "she", "her",
            "hers", "herself", "it", "its", "itself", "they", "them", "their", "theirs", "themselves",
            "what", "which", "who", "whom", "this", "that", "these", "those", "am", "is", "are", "was",
            "were", "be", "been", "being", "have", "has", "had", "having", "do", "does", "did", "doing",
            "a", "an", "the", "and", "but", "if", "or", "because", "as", "until", "while", "of", "at",
            "by", "for", "with", "about", "against", "between", "into", "through", "during", "before",
            "after", "above", "below", "to", "from", "up", "down", "in", "out", "on", "off", "over",
            "under", "again", "further", "then", "once", "here", "there", "when", "where", "why",
            "how", "all", "any", "both", "each", "few", "more", "most", "other", "some", "such",
            "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very", "s", "t",
            "can", "will", "just", "don", "should", "now"};


    public static final String POSITIVE_WORDS_FILE = "src/main/resources/positive-words.txt";
    public static final String NEGATIVE_WORDS_FILE = "src/main/resources/negative-words.txt";

    public static final String POSITIVE = "Positive";

    public static final String NEGATIVE = "Negative";

    public static final String NEUTRAL = "Neutral";
    public static final String[] excludeChars = {"<", ">", "\"", "&amp;", "&lt;", "&gt;"};


    /**
     * @param contentExtracted extracted text content from the buffer memory
     * @return the clean and sanitized contents, after removing leading / trailing whitespaces and newline
     * @implNote performs Data Transformation for each news content extracted
     */
    public static String getSanitizedContent(String contentExtracted) {
        String sanitizedContent = contentExtracted;
        String exclusionPattern = String.join(ConfigUtils.PIPE_DELIMITER, excludeChars);
        sanitizedContent = sanitizedContent.replaceAll(exclusionPattern, "");

        // Remove non-printable chars
        sanitizedContent = sanitizedContent.replaceAll("&#\\d+;", "");
        return sanitizedContent.trim();
    }
}
