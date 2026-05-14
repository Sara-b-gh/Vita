package com.vita.devora.Entities;

public class NewsArticle {

    private String title;
    private String description;
    private String url;
    private String publishedAt;
    private String sourceName;

    public NewsArticle(String title, String description, String url, String publishedAt, String sourceName) {
        this.title = title;
        this.description = description;
        this.url = url;
        this.publishedAt = publishedAt;
        this.sourceName = sourceName;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getUrl() { return url; }
    public String getPublishedAt() { return publishedAt; }
    public String getSourceName() { return sourceName; }
}