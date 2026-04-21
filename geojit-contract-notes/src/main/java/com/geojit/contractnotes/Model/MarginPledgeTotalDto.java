package com.geojit.contractnotes.Model;

public class MarginPledgeTotalDto {

//    <Unique Id>~ Q~ <Qty>~ <Total Value>~ <HairCut Value>~ <Balance Amount>
    private String uniqueId;        // Field 0
    private String recordType;      // Field 1 - Always "Q"
    private String qty;             // Field 2
    private String totalValue;      // Field 3
    private String hairCutValue;    // Field 4
    private String balanceAmount;   // Field 5

    public MarginPledgeTotalDto() {
    }

    public MarginPledgeTotalDto(String uniqueId, String recordType, String qty, String totalValue,
                                String hairCutValue, String balanceAmount) {
        this.uniqueId = uniqueId;
        this.recordType = recordType;
        this.qty = qty;
        this.totalValue = totalValue;
        this.hairCutValue = hairCutValue;
        this.balanceAmount = balanceAmount;
    }

    public String getUniqueId() { return uniqueId; }
    public void setUniqueId(String uniqueId) { this.uniqueId = uniqueId; }

    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }

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
        return "QuantityDto{" +
                "uniqueId='" + uniqueId + '\'' +
                ", recordType='" + recordType + '\'' +
                ", qty='" + qty + '\'' +
                ", totalValue='" + totalValue + '\'' +
                ", hairCutValue='" + hairCutValue + '\'' +
                ", balanceAmount='" + balanceAmount + '\'' +
                '}';
    }
}