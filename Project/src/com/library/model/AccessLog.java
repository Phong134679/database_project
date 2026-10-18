package com.library.model;

import java.time.LocalDateTime;

/**
 * Entity representing an access/download history log for digital journals.
 */
public class AccessLog {
    private long logId;
    private int personId;
    private int journalResourceId;
    private LocalDateTime accessTimestamp;
    private String ipAddress;
    private boolean downloadSuccess;

    public AccessLog() {
    }

    public AccessLog(long logId, int personId, int journalResourceId,
                     LocalDateTime accessTimestamp, String ipAddress, boolean downloadSuccess) {
        this.logId = logId;
        this.personId = personId;
        this.journalResourceId = journalResourceId;
        this.accessTimestamp = accessTimestamp;
        this.ipAddress = ipAddress;
        this.downloadSuccess = downloadSuccess;
    }

    public long getLogId() {
        return logId;
    }

    public void setLogId(long logId) {
        this.logId = logId;
    }

    public int getPersonId() {
        return personId;
    }

    public void setPersonId(int personId) {
        this.personId = personId;
    }

    public int getJournalResourceId() {
        return journalResourceId;
    }

    public void setJournalResourceId(int journalResourceId) {
        this.journalResourceId = journalResourceId;
    }

    public LocalDateTime getAccessTimestamp() {
        return accessTimestamp;
    }

    public void setAccessTimestamp(LocalDateTime accessTimestamp) {
        this.accessTimestamp = accessTimestamp;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public boolean isDownloadSuccess() {
        return downloadSuccess;
    }

    public void setDownloadSuccess(boolean downloadSuccess) {
        this.downloadSuccess = downloadSuccess;
    }

    @Override
    public String toString() {
        return "AccessLog{" +
                "logId=" + logId +
                ", personId=" + personId +
                ", journalResourceId=" + journalResourceId +
                ", accessTimestamp=" + accessTimestamp +
                ", ipAddress='" + ipAddress + '\'' +
                ", downloadSuccess=" + downloadSuccess +
                '}';
    }
}
