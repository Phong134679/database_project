package com.library.model;

import java.time.LocalDate;

/**
 * Subclass of Resource representing a digital journal or article.
 * Thuộc tính 'doi' đã được bãi bỏ do người dùng truy cập trực tiếp qua url/hệ thống
 * mà không cần mã định danh quốc tế DOI cho từng bản online.
 */
public class DigitalJournal extends Resource {
    private String url;
    private int fileSize;
    private String format;
    private boolean isPublic;

    public DigitalJournal() {
        super();
    }

    public DigitalJournal(int resourceId, String title, LocalDate publishDate, String topic, String resourceType,
                          String url, int fileSize, String format) {
        this(resourceId, title, publishDate, topic, resourceType, url, fileSize, format, false);
    }

    public DigitalJournal(int resourceId, String title, LocalDate publishDate, String topic, String resourceType,
                          String url, int fileSize, String format, boolean isPublic) {
        super(resourceId, title, publishDate, topic, resourceType);
        this.url = url;
        this.fileSize = fileSize;
        this.format = format;
        this.isPublic = isPublic;
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

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    @Override
    public String toString() {
        return "DigitalJournal{" +
                "resourceId=" + getResourceId() +
                ", title='" + getTitle() + '\'' +
                ", url='" + url + '\'' +
                ", fileSize=" + fileSize +
                ", format='" + format + '\'' +
                ", isPublic=" + isPublic +
                '}';
    }
}
