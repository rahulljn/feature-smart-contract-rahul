package com.geojit.contractnotes.Model;


public class SecurityTransactionDto {

//    <Unique Id>~ C~ <SL No>~ <Security>~ <Segment>~ <Quantity>~ <Price>~
//    <Value>~ <STT>~ <Quantity>~ <Price>~ <Value>~ <STT>~ <Quantity>~ <Price>~
//    <Value>~ <STT>~ <Total STT(Rs)>
    private String uniqueId;        // Field 0
    private String recordType;      // Field 1 - Always "C"
    private String slNo;            // Field 2
    private String security;        // Field 3
    private String segment;         // Field 4
    private String quantity1;       // Field 5
    private String price1;          // Field 6
    private String value1;          // Field 7
    private String stt1;            // Field 8
    private String quantity2;       // Field 9
    private String price2;          // Field 10
    private String value2;          // Field 11
    private String stt2;            // Field 12
    private String quantity3;       // Field 13
    private String price3;          // Field 14
    private String value3;          // Field 15
    private String stt3;            // Field 16
    private String totalSttRs;      // Field 17

    public SecurityTransactionDto() {
    }

    public SecurityTransactionDto(String uniqueId, String recordType, String slNo, String security,
                                  String segment, String quantity1, String price1, String value1,
                                  String stt1, String quantity2, String price2, String value2,
                                  String stt2, String quantity3, String price3, String value3,
                                  String stt3, String totalSttRs) {
        this.uniqueId = uniqueId;
        this.recordType = recordType;
        this.slNo = slNo;
        this.security = security;
        this.segment = segment;
        this.quantity1 = quantity1;
        this.price1 = price1;
        this.value1 = value1;
        this.stt1 = stt1;
        this.quantity2 = quantity2;
        this.price2 = price2;
        this.value2 = value2;
        this.stt2 = stt2;
        this.quantity3 = quantity3;
        this.price3 = price3;
        this.value3 = value3;
        this.stt3 = stt3;
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

    public String getSegment() { return segment; }
    public void setSegment(String segment) { this.segment = segment; }

    public String getQuantity1() { return quantity1; }
    public void setQuantity1(String quantity1) { this.quantity1 = quantity1; }

    public String getPrice1() { return price1; }
    public void setPrice1(String price1) { this.price1 = price1; }

    public String getValue1() { return value1; }
    public void setValue1(String value1) { this.value1 = value1; }

    public String getStt1() { return stt1; }
    public void setStt1(String stt1) { this.stt1 = stt1; }

    public String getQuantity2() { return quantity2; }
    public void setQuantity2(String quantity2) { this.quantity2 = quantity2; }

    public String getPrice2() { return price2; }
    public void setPrice2(String price2) { this.price2 = price2; }

    public String getValue2() { return value2; }
    public void setValue2(String value2) { this.value2 = value2; }

    public String getStt2() { return stt2; }
    public void setStt2(String stt2) { this.stt2 = stt2; }

    public String getQuantity3() { return quantity3; }
    public void setQuantity3(String quantity3) { this.quantity3 = quantity3; }

    public String getPrice3() { return price3; }
    public void setPrice3(String price3) { this.price3 = price3; }

    public String getValue3() { return value3; }
    public void setValue3(String value3) { this.value3 = value3; }

    public String getStt3() { return stt3; }
    public void setStt3(String stt3) { this.stt3 = stt3; }

    public String getTotalSttRs() { return totalSttRs; }
    public void setTotalSttRs(String totalSttRs) { this.totalSttRs = totalSttRs; }

    @Override
    public String toString() {
        return "ContractDto{" +
                "uniqueId='" + uniqueId + '\'' +
                ", recordType='" + recordType + '\'' +
                ", slNo='" + slNo + '\'' +
                ", security='" + security + '\'' +
                ", totalSttRs='" + totalSttRs + '\'' +
                '}';
    }
}