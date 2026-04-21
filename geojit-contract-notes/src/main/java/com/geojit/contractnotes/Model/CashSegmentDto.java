package com.geojit.contractnotes.Model;


public class CashSegmentDto {

//<Unique Id>~ L~ <SL No.>~ <Security>~ <Expiry Date>~ <Sale>~ <STT>~ <Sale>~ <STT>~
// <Total STT(Rs)>
    private String uniqueId;        // Field 0
    private String recordType;      // Field 1 - Always "L"
    private String slNo;            // Field 2
    private String security;        // Field 3
    private String expiryDate;      // Field 4
    private String sale1;           // Field 5
    private String stt1;            // Field 6
    private String sale2;           // Field 7
    private String stt2;            // Field 8
    private String totalSttRs;      // Field 9

    public CashSegmentDto() {
    }

    public CashSegmentDto(String uniqueId, String recordType, String slNo, String security,
                          String expiryDate, String sale1, String stt1, String sale2,
                          String stt2, String totalSttRs) {
        this.uniqueId = uniqueId;
        this.recordType = recordType;
        this.slNo = slNo;
        this.security = security;
        this.expiryDate = expiryDate;
        this.sale1 = sale1;
        this.stt1 = stt1;
        this.sale2 = sale2;
        this.stt2 = stt2;
        this.totalSttRs = totalSttRs;
    }

    public String getUniqueId() { return uniqueId; }
    public void setUniqueId(String uniqueId) { this.uniqueId = uniqueId; }

    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }

    public String getSlNo() { return slNo; }
    public void setSlNo(String slNo) { this.slNo = slNo; }

    public String getSecurity() { return security; }
    public void setSecurity(String security) { this.security = security; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public String getSale1() { return sale1; }
    public void setSale1(String sale1) { this.sale1 = sale1; }

    public String getStt1() { return stt1; }
    public void setStt1(String stt1) { this.stt1 = stt1; }

    public String getSale2() { return sale2; }
    public void setSale2(String sale2) { this.sale2 = sale2; }

    public String getStt2() { return stt2; }
    public void setStt2(String stt2) { this.stt2 = stt2; }

    public String getTotalSttRs() { return totalSttRs; }
    public void setTotalSttRs(String totalSttRs) { this.totalSttRs = totalSttRs; }

    @Override
    public String toString() {
        return "LotDto{" +
                "uniqueId='" + uniqueId + '\'' +
                ", recordType='" + recordType + '\'' +
                ", slNo='" + slNo + '\'' +
                ", security='" + security + '\'' +
                ", expiryDate='" + expiryDate + '\'' +
                ", totalSttRs='" + totalSttRs + '\'' +
                '}';
    }
}