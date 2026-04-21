package com.geojit.contractnotes.Model;

public class DerivativeSegmentDto {

    //<Unique Id>~ V~ <Contract Description>~ <Buy[B]/Sell[S]>~
    // <Quantity>~ <WAP per unit(In foreign Currency)>~
    // <WAP per unit(Rs)>~ <Brokerage Per Unit(Rs)>~
    // <WAP per unit after Brokerage(Rs)>~ <Closing Rate per unit>~
    // <Net total>~ <Remarks>

    private String uniqueId;                   // Field 0
    private String recordType;                 // Field 1 - Always "V"
    private String contractDescription;        // Field 2
    private String buySell;                    // Field 3
    private String quantity;                   // Field 4
    private String wapPerUnitForeignCurrency;  // Field 5
    private String wapPerUnitRs;               // Field 6
    private String brokeragePerUnitRs;         // Field 7
    private String wapPerUnitAfterBrokerageRs; // Field 8
    private String closingRatePerUnit;         // Field 9
    private String netTotal;                   // Field 10
    private String remarks;                    // Field 11

    public DerivativeSegmentDto() {}

    public DerivativeSegmentDto(String uniqueId, String recordType, String contractDescription, String buySell,
                                String quantity, String wapPerUnitForeignCurrency, String wapPerUnitRs,
                                String brokeragePerUnitRs, String wapPerUnitAfterBrokerageRs,
                                String closingRatePerUnit, String netTotal, String remarks) {
        this.uniqueId = uniqueId;
        this.recordType = recordType;
        this.contractDescription = contractDescription;
        this.buySell = buySell;
        this.quantity = quantity;
        this.wapPerUnitForeignCurrency = wapPerUnitForeignCurrency;
        this.wapPerUnitRs = wapPerUnitRs;
        this.brokeragePerUnitRs = brokeragePerUnitRs;
        this.wapPerUnitAfterBrokerageRs = wapPerUnitAfterBrokerageRs;
        this.closingRatePerUnit = closingRatePerUnit;
        this.netTotal = netTotal;
        this.remarks = remarks;
    }

    public String getUniqueId() { return uniqueId; }
    public void setUniqueId(String uniqueId) { this.uniqueId = uniqueId; }

    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }

    public String getContractDescription() { return contractDescription; }
    public void setContractDescription(String contractDescription) { this.contractDescription = contractDescription; }

    public String getBuySell() { return buySell; }
    public void setBuySell(String buySell) { this.buySell = buySell; }

    public String getQuantity() { return quantity; }
    public void setQuantity(String quantity) { this.quantity = quantity; }

    public String getWapPerUnitForeignCurrency() { return wapPerUnitForeignCurrency; }
    public void setWapPerUnitForeignCurrency(String wapPerUnitForeignCurrency) { this.wapPerUnitForeignCurrency = wapPerUnitForeignCurrency; }

    public String getWapPerUnitRs() { return wapPerUnitRs; }
    public void setWapPerUnitRs(String wapPerUnitRs) { this.wapPerUnitRs = wapPerUnitRs; }

    public String getBrokeragePerUnitRs() { return brokeragePerUnitRs; }
    public void setBrokeragePerUnitRs(String brokeragePerUnitRs) { this.brokeragePerUnitRs = brokeragePerUnitRs; }

    public String getWapPerUnitAfterBrokerageRs() { return wapPerUnitAfterBrokerageRs; }
    public void setWapPerUnitAfterBrokerageRs(String wapPerUnitAfterBrokerageRs) { this.wapPerUnitAfterBrokerageRs = wapPerUnitAfterBrokerageRs; }

    public String getClosingRatePerUnit() { return closingRatePerUnit; }
    public void setClosingRatePerUnit(String closingRatePerUnit) { this.closingRatePerUnit = closingRatePerUnit; }

    public String getNetTotal() { return netTotal; }
    public void setNetTotal(String netTotal) { this.netTotal = netTotal; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    @Override
    public String toString() {
        return "VDto{" +
                "uniqueId='" + uniqueId + '\'' +
                ", recordType='" + recordType + '\'' +
                ", contractDescription='" + contractDescription + '\'' +
                ", buySell='" + buySell + '\'' +
                ", quantity='" + quantity + '\'' +
                ", netTotal='" + netTotal + '\'' +
                '}';
    }
}