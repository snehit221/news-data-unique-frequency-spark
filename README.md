# Reuters News Frequency Counter

## ETL

The first phase is performed using Extract, Transform and Load (ETL) operations on two given
news files, namely "reut2-009.sgm" and "reut2-014.sgm”. The data is extracted from the two
given files, cleaned (and transformed), and loaded into the MongoDB database named "ReuterDb."

## Overview
The **Reuters News Frequency Counter** is a Java-based Spark application that processes and analyzes Reuters news data. The application extracts and transforms textual content, calculates word frequencies, and outputs sorted word counts in descending order. It runs on a **YARN cluster** and leverages **Apache Spark** for efficient distributed processing.

## Features
- Reads Reuters news data from input files.
- Extracts text content using **regex pattern matching**.
- Cleans and tokenizes data for further processing.
- Computes the frequency of unique words.
- Sorts words by frequency in **descending order**.
- Stores the results in a single output file.

## System Architecture & Data Flow

1. **Initialize Spark Configuration**
   - Sets up the **SparkConf** object.
   - Defines the application name as `reutersFrequencyCounter`.
   - Specifies that the application will run on a **YARN cluster**.
   
   ```java
   public static SparkConf getSparkConfig() {
       return new SparkConf().setAppName("reutersFrequencyCounter").setMaster("yarn");
   }
   ```

2. **Read Input Reuters News Data**
   - Reads the input file using SparkContext’s `textFile` method.

3. **Extraction Engine**
   - Implements `flatMapFunction` interface.
   - Uses `ReutersTextExtractor` class to extract text enclosed within `<TEXT>` tags.
   - Parses fields like **Author, Dateline, Title, and Body**.
   - Stores extracted data in an intermediate buffer (`extractedPreCleanedBuffer`).

4. **Data Transformation Engine**
   - Converts extracted text to **lowercase**.
   - Cleans the text by removing special characters and **stop words**.
   - Tokenizes cleaned words and prepares data for frequency computation.

5. **Calculate Unique Word Frequencies**
   - Maps words into **(word, 1)** key-value pairs.
   - Uses `reduceByKey` to compute the total frequency of each unique word.
   
   ```java
   JavaPairRDD<String, Integer> wordCounts = reutersNewsTextRDD
       .mapToPair(word -> new Tuple2<>(word, 1))
       .reduceByKey(Integer::sum);
   ```

6. **Sort Words by Frequency in Descending Order**
   - Swaps key-value pairs `(word, count) → (count, word)`.
   - Applies `sortByKey(false)` to sort in descending order.
   - Swaps key-value pairs back to `(word, count)`.
   
   ```java
   JavaPairRDD<String, Integer> sortedWordCounts = wordCounts
       .mapToPair(Tuple2::swap) // Swap (word, count) → (count, word)
       .sortByKey(false) // Sort in descending order
       .mapToPair(Tuple2::swap); // Swap back to (word, count)
   ```

7. **Load Results**
   - Stores sorted word counts in an output text file.
   - Uses `coalesce(1)` to ensure results are stored in a **single file**.

