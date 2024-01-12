package org.example;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.*;
import com.mongodb.client.result.InsertManyResult;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.example.utils.ConfigUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

/**
 * performs ETL operation for persisting reuters news to MongoDB
 */
public class ReutRead {

    private final List<ReutersNews> reutersNewsList = new LinkedList<>();
    private File[] filePath;

    public ReutRead(File[] filePath) {
        this.filePath = filePath;
    }

    public ReutRead() {
    }

    public static void main(String[] args) {
        ReutRead reutRead = new ReutRead(createFilePaths());

        reutRead.processReutersNewsFiles();

        //load it to the database
        reutRead.loadReutersNewsToMongoDB(reutRead.getReutersNewsList());
    }

    /**
     * @return the array with the file paths to be read for performing data extraction
     */
    protected static File[] createFilePaths() {
        return new File[]{new File(ConfigUtils.REUTERS_NEWS_FILE1), new File(ConfigUtils.REUTERS_NEWS_FILE2)};
    }


    /**
     * @return retrieves ReutersNews collection
     * @implNote establishes connection to MongoDB atlas cloud
     */
    private static MongoCollection<ReutersNews> getReutersNewsMongoCollection() {
        ConnectionString mongoUri = new ConnectionString(ConfigUtils.MONGO_CONNECTION_URL);
        String dbName = ConfigUtils.MONGO_DATABASE_NAME;
        String collectionName = ConfigUtils.MONGO_COLLECTION_NAME;
        CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));

        MongoClientSettings settings = MongoClientSettings.builder()
                .codecRegistry(pojoCodecRegistry)
                .applyConnectionString(mongoUri).build();

        MongoClient mongoClient = null;
        try {
            mongoClient = MongoClients.create(settings);
        } catch (MongoException mongoException) {
            System.err.println("Unable to connect to the MongoDB instance due to an error: " + mongoException);
            System.exit(1);
        }

        MongoDatabase database = mongoClient.getDatabase(dbName);

        return database.getCollection(collectionName, ReutersNews.class);
    }

    /**
     * Retrieves all ReutersNews records from the MongoDB collection.
     *
     * @return List of ReutersNews objects
     * @implNote uses the find method of the MongoCollection class
     */
    protected List<ReutersNews> getAllReutersNewsFromMongoDB() {
        MongoCollection<ReutersNews> collection = getReutersNewsMongoCollection();

        try {
            FindIterable<ReutersNews> result = collection.find();

            result.forEach(reutersNewsList::add);

        } catch (MongoException mongoException) {
            System.err.println("Exception occurred during retrieval Reuters News from MongoDB:  " + mongoException);
            System.exit(1);
        }

        return reutersNewsList;
    }

    public List<ReutersNews> getReutersNewsList() {
        return reutersNewsList;
    }

    /**
     * responsible for performing the extract operation for Reuters News Files
     */
    protected void processReutersNewsFiles() {
        File[] files = getFilePath();
        for (File fileName : files) {
            if (fileName.exists()) {
                try {
                    extractDataFromNewsFile(fileName);   // EXTRACT
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                System.out.println("Invalid File Path!");
            }

        }
    }

    /**
     * @param reutersNewsList list of news objects, which needs to be inserted
     * @implNote responsible for performing the load operation for ReutersNews in Mongo
     */
    private void loadReutersNewsToMongoDB(List<ReutersNews> reutersNewsList) {
        MongoCollection<ReutersNews> collection = getReutersNewsMongoCollection();
        // load / insert data into mongoDB collection
        try {
            InsertManyResult result = collection.insertMany(reutersNewsList);
            System.out.println("***** Successfully Performed the ETL operation program to persist Reuters News to MongoDB *****\n");
            System.out.println("Inserted " + result.getInsertedIds().size() + " documents into mongo collection: " + ConfigUtils.MONGO_COLLECTION_NAME);
            reutersNewsList.clear();
        } catch (MongoException mongoException) {
            System.err.println("Unable to insert any Reuters News into MongoDB due to an error: " + mongoException);
            System.exit(1);
        }
    }

    /**
     * @return the array of file path
     */
    public File[] getFilePath() {
        return filePath;
    }

    /**
     * populates the title and body if matched, in the reutersNews interim buffer
     *
     * @param titleMatcher used for matching the title of the news
     * @param bodyMatcher  used for matching the body of the news
     */
    private void populateExtractedNewsDetails(Matcher titleMatcher, Matcher bodyMatcher) {
        ReutersNews reutersNews = new ReutersNews();
        if (titleMatcher.find()) {
            String titleExtracted = titleMatcher.group(1);
            //System.out.println("title: " + titleExtracted);
            reutersNews.setTitle(ConfigUtils.getSanitizedContent(titleExtracted));   // Data Transformation
        }
        if (bodyMatcher.find()) {
            //System.out.println("BODY matches!!!!!");
            String bodyExtracted = bodyMatcher.group(1);
            // System.out.println("body: " + bodyExtracted);
            reutersNews.setBody(ConfigUtils.getSanitizedContent(bodyExtracted));   // Data Transformation
        }
        if (!isEmpty(reutersNews)) {
            reutersNewsList.add(reutersNews);
        }
    }

    /**
     * @return true if both title and body is null, false otherwise
     */
    private boolean isEmpty(ReutersNews reutersNews) {
        return reutersNews.getTitle() == null && reutersNews.getBody() == null;
    }

    /**
     * extracts the data from sgm files
     *
     * @param sgmNewsFile the reuters News raw file
     * @throws IOException If an I/O error occurs while reading the file.
     */
    private void extractDataFromNewsFile(File sgmNewsFile) throws IOException {

        BufferedReader newsReader = new BufferedReader(new FileReader(sgmNewsFile));

        Pattern titlePattern = Pattern.compile("<TITLE>(.*?)</TITLE>", Pattern.DOTALL);
        Pattern bodyPattern = Pattern.compile("<BODY>(.*?)</BODY>", Pattern.DOTALL);
        String eachReutersNewsLine;
        StringBuilder eachNewsBuffer = new StringBuilder();

        while ((eachReutersNewsLine = newsReader.readLine()) != null) {

            if (!eachReutersNewsLine.contains(ConfigUtils.REUTERS_END_TAG)) {
                eachNewsBuffer.append(eachReutersNewsLine).append(ConfigUtils.LINE_SEPARATOR);
                continue;
            }
            // now we have eachNewsBuffer to operate upon
            Matcher titleMatcher = titlePattern.matcher(eachNewsBuffer);
            Matcher bodyMatcher = bodyPattern.matcher(eachNewsBuffer);

            populateExtractedNewsDetails(titleMatcher, bodyMatcher);
            // reset the buffer
            eachNewsBuffer = new StringBuilder();
        }
    }
}
