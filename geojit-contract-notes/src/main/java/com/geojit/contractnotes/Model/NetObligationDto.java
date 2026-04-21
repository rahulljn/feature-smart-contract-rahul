package com.geojit.contractnotes.Model;

public class NetObligationDto {

//    <Unique Id>~ M~ <Sl No.>~ <Security>~ <Segment>~ <Quantity>~
//    <Rate>~ <Quantity>~ <Rate>~ <Quantity>~ <Rate>~ <Amount>
    private String uniqueId;        // Field 0
    private String recordType;      // Field 1 - Always "M"
    private String slNo;            // Field 2
    private String security;        // Field 3
    private String segment;         // Field 4
    private String quantity1;       // Field 5
    private String rate1;           // Field 6
    private String quantity2;       // Field 7
    private String rate2;           // Field 8
    private String quantity3;       // Field 9
    private String rate3;           // Field 10
    private String amount;          // Field 11

    public NetObligationDto() {
    }

    public NetObligationDto(String uniqueId, String recordType, String slNo, String security,
                            String segment, String quantity1, String rate1, String quantity2,
                            String rate2, String quantity3, String rate3, String amount) {
        this.uniqueId = uniqueId;
        this.recordType = recordType;
        this.slNo = slNo;
        this.security = security;
        this.segment = segment;
        this.quantity1 = quantity1;
        this.rate1 = rate1;
        this.quantity2 = quantity2;
        this.rate2 = rate2;
        this.quantity3 = quantity3;
        this.rate3 = rate3;
        this.amount = amount;
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

    public String getRate1() { return rate1; }
    public void setRate1(String rate1) { this.rate1 = rate1; }

    public String getQuantity2() { return quantity2; }
    public void setQuantity2(String quantity2) { this.quantity2 = quantity2; }

    public String getRate2() { return rate2; }
    public void setRate2(String rate2) { this.rate2 = rate2; }

    public String getQuantity3() { return quantity3; }
    public void setQuantity3(String quantity3) { this.quantity3 = quantity3; }

    public String getRate3() { return rate3; }
    public void setRate3(String rate3) { this.rate3 = rate3; }

    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }

    @Override
    public String toString() {
        return "MarginDto{" +
                "uniqueId='" + uniqueId + '\'' +
                ", recordType='" + recordType + '\'' +
                ", slNo='" + slNo + '\'' +
                ", security='" + security + '\'' +
                ", amount='" + amount + '\'' +
                '}';
    }
}