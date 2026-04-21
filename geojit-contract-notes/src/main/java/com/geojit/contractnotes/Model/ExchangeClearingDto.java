package com.geojit.contractnotes.Model;


public class ExchangeClearingDto {

//<Unique Id>~ E~ <Exchange/Clearing Corporation>~ <Segment>~ <STTLNO>~ <STTLDATE>~ <UCCODE>
    private String uniqueId;                    // Field 0
    private String recordType;                  // Field 1 - Always "E"
    private String exchangeClearingCorporation; // Field 2
    private String segment;                     // Field 3
    private String settlementNo;                // Field 4
    private String settlementDate;              // Field 5
    private String ucCode;                      // Field 6

    // Constructors
    public ExchangeClearingDto() {
    }

    public ExchangeClearingDto(String uniqueId, String recordType, String exchangeClearingCorporation,
                               String segment, String settlementNo, String settlementDate, String ucCode) {
        this.uniqueId = uniqueId;
        this.recordType = recordType;
        this.exchangeClearingCorporation = exchangeClearingCorporation;
        this.segment = segment;
        this.settlementNo = settlementNo;
        this.settlementDate = settlementDate;
        this.ucCode = ucCode;
    }

    // Getters and Setters
    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    public String getExchangeClearingCorporation() {
        return exchangeClearingCorporation;
    }

    public void setExchangeClearingCorporation(String exchangeClearingCorporation) {
        this.exchangeClearingCorporation = exchangeClearingCorporation;
    }

    public String getSegment() {
        return segment;
    }

    public void setSegment(String segment) {
        this.segment = segment;
    }

    public String getSettlementNo() {
        return settlementNo;
    }

    public void setSettlementNo(String settlementNo) {
        this.settlementNo = settlementNo;
    }

    public String getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(String settlementDate) {
        this.settlementDate = settlementDate;
    }

    public String getUcCode() {
        return ucCode;
    }

    public void setUcCode(String ucCode) {
        this.ucCode = ucCode;
    }

    @Override
    public String toString() {
        return "ExchangeDto{" +
                "uniqueId='" + uniqueId + '\'' +
                ", recordType='" + recordType + '\'' +
                ", exchangeClearingCorporation='" + exchangeClearingCorporation + '\'' +
                ", segment='" + segment + '\'' +
                ", settlementNo='" + settlementNo + '\'' +
                ", settlementDate='" + settlementDate + '\'' +
                ", ucCode='" + ucCode + '\'' +
                '}';
    }
}