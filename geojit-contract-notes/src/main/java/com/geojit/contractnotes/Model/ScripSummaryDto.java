package com.geojit.contractnotes.Model;


public class ScripSummaryDto {

//    <Unique Id>~ U~ <Security Description>~ <B/S>~ <Quantity>~
//    <Gross Rate Per Security>~ <Gross Total(Rs)>~ <Gross Brokerage Per Security>~
//    <Brokerage(Total)>~ <Net Rate(Rs)>~ <Net Total Amount(Rs)>
    private String uniqueId;                   // Field 0
    private String recordType;                 // Field 1 - Always "U"
    private String securityDescription;        // Field 2
    private String buySell;                    // Field 3
    private String quantity;                   // Field 4
    private String grossRatePerSecurity;       // Field 5
    private String grossTotalRs;               // Field 6
    private String grossBrokeragePerSecurity;  // Field 7
    private String brokerageTotal;             // Field 8
    private String netRateRs;                  // Field 9
    private String netTotalAmountRs;           // Field 10

    public ScripSummaryDto() {}

    public ScripSummaryDto(String uniqueId, String recordType, String securityDescription, String buySell,
                           String quantity, String grossRatePerSecurity, String grossTotalRs,
                           String grossBrokeragePerSecurity, String brokerageTotal,
                           String netRateRs, String netTotalAmountRs) {
        this.uniqueId = uniqueId;
        this.recordType = recordType;
        this.securityDescription = securityDescription;
        this.buySell = buySell;
        this.quantity = quantity;
        this.grossRatePerSecurity = grossRatePerSecurity;
        this.grossTotalRs = grossTotalRs;
        this.grossBrokeragePerSecurity = grossBrokeragePerSecurity;
        this.brokerageTotal = brokerageTotal;
        this.netRateRs = netRateRs;
        this.netTotalAmountRs = netTotalAmountRs;
    }

    public String getUniqueId() { return uniqueId; }
    public void setUniqueId(String uniqueId) { this.uniqueId = uniqueId; }

    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }

    public String getSecurityDescription() { return securityDescription; }
    public void setSecurityDescription(String securityDescription) { this.securityDescription = securityDescription; }

    public String getBuySell() { return buySell; }
    public void setBuySell(String buySell) { this.buySell = buySell; }

    public String getQuantity() { return quantity; }
    public void setQuantity(String quantity) { this.quantity = quantity; }

    public String getGrossRatePerSecurity() { return grossRatePerSecurity; }
    public void setGrossRatePerSecurity(String grossRatePerSecurity) { this.grossRatePerSecurity = grossRatePerSecurity; }

    public String getGrossTotalRs() { return grossTotalRs; }
    public void setGrossTotalRs(String grossTotalRs) { this.grossTotalRs = grossTotalRs; }

    public String getGrossBrokeragePerSecurity() { return grossBrokeragePerSecurity; }
    public void setGrossBrokeragePerSecurity(String grossBrokeragePerSecurity) { this.grossBrokeragePerSecurity = grossBrokeragePerSecurity; }

    public String getBrokerageTotal() { return brokerageTotal; }
    public void setBrokerageTotal(String brokerageTotal) { this.brokerageTotal = brokerageTotal; }

    public String getNetRateRs() { return netRateRs; }
    public void setNetRateRs(String netRateRs) { this.netRateRs = netRateRs; }

    public String getNetTotalAmountRs() { return netTotalAmountRs; }
    public void setNetTotalAmountRs(String netTotalAmountRs) { this.netTotalAmountRs = netTotalAmountRs; }

    @Override
    public String toString() {
        return "UDto{" +
                "uniqueId='" + uniqueId + '\'' +
                ", recordType='" + recordType + '\'' +
                ", securityDescription='" + securityDescription + '\'' +
                ", buySell='" + buySell + '\'' +
                ", quantity='" + quantity + '\'' +
                ", netTotalAmountRs='" + netTotalAmountRs + '\'' +
                '}';
    }
}