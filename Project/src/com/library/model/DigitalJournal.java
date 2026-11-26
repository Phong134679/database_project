package com.library.model;

/**
 * Subclass of Resource representing a digital journal or article.
 */
public class DigitalJournal extends Resource {
    private String url;
    private int fileSize;
    private String format;

    public DigitalJournal() {
        super();
    }

    public DigitalJournal(int resourceId, String title, String topic, boolean isPublic,
                          String url, int fileSize, String format) {
        super(resourceId, title, topic, isPublic);
        this.url = url;
        this.fileSize = fileSize;
        this.format = format;
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
                ", url='" + url + '\'' +
                ", fileSize=" + fileSize +
                ", format='" + format + '\'' +
                ", isPublic=" + isPublic() +
                '}';
    }
}
