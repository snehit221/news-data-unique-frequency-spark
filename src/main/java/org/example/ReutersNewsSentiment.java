package org.example;

/**
 * class for storing the sentiment details of each title which needs to be pushed to the DB
 */
public class ReutersNewsSentiment {

    private Integer newsId;
    private String newsTitleContent;
    private String wordMatch;
    private Integer score;
    private String polarity;

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getNewsTitleContent() {
        return newsTitleContent;
    }

    public void setNewsTitleContent(String newsTitleContent) {
        this.newsTitleContent = newsTitleContent;
    }

    public String getWordMatch() {
        return wordMatch;
    }

    public void setWordMatch(String wordMatch) {
        this.wordMatch = wordMatch;
    }

    public String getPolarity() {
        return polarity;
    }

    public void setPolarity(String polarity) {
        this.polarity = polarity;
    }

    @Override
    public String toString() {
        return "ReutersNewsSentiment{" +
                "newsId=" + newsId +
                ", newsTitleContent='" + newsTitleContent + '\'' +
                ", wordMatch='" + wordMatch + '\'' +
                ", score=" + score +
                ", polarity='" + polarity + '\'' +
                '}';
    }
}
