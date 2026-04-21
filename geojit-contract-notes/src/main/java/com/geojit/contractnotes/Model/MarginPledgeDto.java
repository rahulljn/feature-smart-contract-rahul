package com.geojit.contractnotes.Model;


public class MarginPledgeDto {
//    <Unique Id>~ K~ <Security>~ <Qty>~ <Total Value>~ <HairCut Value>~ <Balance Amount>
    private String uniqueId;        // Field 0
    private String recordType;      // Field 1 - Always "K"
    private String security;        // Field 2
    private String qty;             // Field 3
    private String totalValue;      // Field 4
    private String hairCutValue;    // Field 5
    private String balanceAmount;   // Field 6

    public MarginPledgeDto() {
    }

    public MarginPledgeDto(String uniqueId, String recordType, String security, String qty,
                           String totalValue, String hairCutValue, String balanceAmount) {
        this.uniqueId = uniqueId;
        this.recordType = recordType;
        this.security = security;
        this.qty = qty;
        this.totalValue = totalValue;
        this.hairCutValue = hairCutValue;
        this.balanceAmount = balanceAmount;
    }

    public String getUniqueId() { return uniqueId; }
    public void setUniqueId(String uniqueId) { this.uniqueId = uniqueId; }

    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }

    public String getSecurity() { return security; }
    public void setSecurity(String security) { this.security = security; }

    public String getQty() { return qty; }
    public void setQty(String qty) { this.qty = qty; }

    public String getTotalValue() { return totalValue; }
    public void setTotalValue(String totalValue) { this.totalValue = totalValue; }

    public String getHairCutValue() { return hairCutValue; }
    public void setHairCutValue(String hairCutValue) { this.hairCutValue = hairCutValue; }

    public String getBalanceAmount() { return balanceAmount; }
    public void setBalanceAmount(String balanceAmount) { this.balanceAmount = balanceAmount; }

    @Override
    public String toString() {
        return "SecurityDto{" +
                "uniqueId='" + uniqueId + '\'' +
                ", recordType='" + recordType + '\'' +
                ", security='" + security + '\'' +
                ", qty='" + qty + '\'' +
                ", totalValue='" + totalValue + '\'' +
                ", hairCutValue='" + hairCutValue + '\'' +
                ", balanceAmount='" + balanceAmount + '\'' +
                '}';
    }
}