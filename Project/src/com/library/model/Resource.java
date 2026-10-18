package com.library.model;

import java.time.LocalDate;

/**
 * Superclass representing the master catalog of all material types (physical or digital).
 */
public class Resource {
    private int resourceId;
    private String title;
    private LocalDate publishDate;
    private String topic;
    private String resourceType;

    public Resource() {
    }

    public Resource(int resourceId, String title, LocalDate publishDate, String topic, String resourceType) {
        this.resourceId = resourceId;
        this.title = title;
        this.publishDate = publishDate;
        this.topic = topic;
        this.resourceType = resourceType;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(LocalDate publishDate) {
        this.publishDate = publishDate;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    @Override
    public String toString() {
        return "Resource{" +
                "resourceId=" + resourceId +
                ", title='" + title + '\'' +
                ", publishDate=" + publishDate +
                ", topic='" + topic + '\'' +
                ", resourceType='" + resourceType + '\'' +
                '}';
    }
}
