package org.example;

/**
 * POJO class created for storing each news a document in mongoDB
 */
public class ReutersNews {

    private String title; // title of the news
    private String body; // news body

    public ReutersNews() {

    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "ReutersNews{" +
                "title='" + title + '\'' +
                ", body='" + body + '\'' +
                '}';
    }
}
