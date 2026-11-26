package com.library.model;

/**
 * Superclass representing the master catalog of all material types (physical or digital).
 * Subtype tables (PHYSICAL_BOOK, DIGITAL_JOURNAL) determine the resource kind.
 */
public class Resource {
    private int resourceId;
    private String title;
    private String topic;
    private boolean isPublic;

    public Resource() {
    }

    public Resource(int resourceId, String title, String topic, boolean isPublic) {
        this.resourceId = resourceId;
        this.title = title;
        this.topic = topic;
        this.isPublic = isPublic;
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

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    @Override
    public String toString() {
        return "Resource{" +
                "resourceId=" + resourceId +
                ", title='" + title + '\'' +
                ", topic='" + topic + '\'' +
                ", isPublic=" + isPublic +
                '}';
    }
}
