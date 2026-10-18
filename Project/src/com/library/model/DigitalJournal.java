package com.library.model;

import java.time.LocalDate;

/**
 * Subclass of Resource representing a digital journal or article.
 */
public class DigitalJournal extends Resource {
    private String doi;
    private String url;
    private int fileSize;
    private String format;

    public DigitalJournal() {
        super();
    }

    public DigitalJournal(int resourceId, String title, LocalDate publishDate, String topic, String resourceType,
                          String doi, String url, int fileSize, String format) {
        super(resourceId, title, publishDate, topic, resourceType);
        this.doi = doi;
        this.url = url;
        this.fileSize = fileSize;
        this.format = format;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    public String toString() {
        return "DigitalJournal{" +
                "resourceId=" + getResourceId() +
                ", title='" + getTitle() + '\'' +
                ", doi='" + doi + '\'' +
                ", url='" + url + '\'' +
                ", fileSize=" + fileSize +
                ", format='" + format + '\'' +
                '}';
    }
}
