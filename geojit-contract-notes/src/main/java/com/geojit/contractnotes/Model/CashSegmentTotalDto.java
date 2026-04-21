package com.geojit.contractnotes.Model;


public class CashSegmentTotalDto {

    //<Unique Id>~ A~ <Name of Exchange>~ <Segment>~ <Total(Rounded to nearest Rupee)>
    private String uniqueId;                    // Field 0
    private String recordType;                  // Field 1 - Always "A"
    private String nameOfExchange;              // Field 2
    private String segment;                     // Field 3
    private String totalRoundedToNearestRupee;  // Field 4

    public CashSegmentTotalDto() {
    }

    public CashSegmentTotalDto(String uniqueId, String recordType, String nameOfExchange,
                               String segment, String totalRoundedToNearestRupee) {
        this.uniqueId = uniqueId;
        this.recordType = recordType;
        this.nameOfExchange = nameOfExchange;
        this.segment = segment;
        this.totalRoundedToNearestRupee = totalRoundedToNearestRupee;
    }

    public String getUniqueId() { return uniqueId; }
    public void setUniqueId(String uniqueId) { this.uniqueId = uniqueId; }

    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }

    public String getNameOfExchange() { return nameOfExchange; }
    public void setNameOfExchange(String nameOfExchange) { this.nameOfExchange = nameOfExchange; }

    public String getSegment() { return segment; }
    public void setSegment(String segment) { this.segment = segment; }

    public String getTotalRoundedToNearestRupee() { return totalRoundedToNearestRupee; }
    public void setTotalRoundedToNearestRupee(String totalRoundedToNearestRupee) { this.totalRoundedToNearestRupee = totalRoundedToNearestRupee; }

    @Override
    public String toString() {
        return "AmountDto{" +
                "uniqueId='" + uniqueId + '\'' +
                ", recordType='" + recordType + '\'' +
                ", nameOfExchange='" + nameOfExchange + '\'' +
                ", segment='" + segment + '\'' +
                ", totalRoundedToNearestRupee='" + totalRoundedToNearestRupee + '\'' +
                '}';
    }
}