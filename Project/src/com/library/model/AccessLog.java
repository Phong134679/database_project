package com.library.model;

/**
 * Entity representing an access history log for digital journals.
 * Đã bãi bỏ person_id (chỉ ghi nhận ip_address), access_timestamp, và download_success (chỉ đọc trực tuyến).
 */
public class AccessLog {
    private long logId;
    private int journalResourceId;
    private String ipAddress;

    public AccessLog() {
    }

    public AccessLog(long logId, int journalResourceId, String ipAddress) {
        this.logId = logId;
        this.journalResourceId = journalResourceId;
        this.ipAddress = ipAddress;
    }

    public long getLogId() {
        return logId;
    }

    public void setLogId(long logId) {
        this.logId = logId;
    }

    public int getJournalResourceId() {
        return journalResourceId;
    }

    public void setJournalResourceId(int journalResourceId) {
        this.journalResourceId = journalResourceId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Override
    public String toString() {
        return "AccessLog{" +
                "logId=" + logId +
                ", journalResourceId=" + journalResourceId +
                ", ipAddress='" + ipAddress + '\'' +
                '}';
    }
}
