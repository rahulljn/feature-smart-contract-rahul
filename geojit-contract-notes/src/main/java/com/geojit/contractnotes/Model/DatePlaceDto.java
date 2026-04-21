package com.geojit.contractnotes.Model;


public class DatePlaceDto {
    private String uniqueId;    // Field 0
    private String recordType;  // Field 1 - Always "F"
    private String date;        // Field 2
    private String place;       // Field 3

    public DatePlaceDto() {
    }

    public DatePlaceDto(String uniqueId, String recordType, String date, String place) {
        this.uniqueId = uniqueId;
        this.recordType = recordType;
        this.date = date;
        this.place = place;
    }

    public String getUniqueId() { return uniqueId; }
    public void setUniqueId(String uniqueId) { this.uniqueId = uniqueId; }

    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getPlace() { return place; }
    public void setPlace(String place) { this.place = place; }

    @Override
    public String toString() {
        return "FooterDto{" +
                "uniqueId='" + uniqueId + '\'' +
                ", recordType='" + recordType + '\'' +
                ", date='" + date + '\'' +
                ", place='" + place + '\'' +
                '}';
    }
}