package com.geojit.contractnotes.Model;


public class NameClearingCorporationDto {

    private String uniqueId;                         // Field 0
    private String recordType;                       // Field 1 - Always "D"
    private String orderNo;                          // Field 2  - Order No.
    private String orderTime;                        // Field 3  - Order Time
    private String tradeNo;                          // Field 4  - Trade No.
    private String tradeTime;                        // Field 5  - Trade Time
    private String securityContractDescription;      // Field 6  - Security/Contract Description
    private String buySell;                          // Field 7  - Buy/Sell
    private String quantity;                         // Field 8  - Quantity
    private String grossRatePricePerUnitForeignCcy;  // Field 9  - Gross Rate/Trade Price Per Unit (foreign currency)
    private String grossRatePricePerUnitRs;          // Field 10 - Gross Rate/Trade Price Per Unit (Rs)
    private String brokerageRs;                      // Field 11 - #Brokerage (Rs)
    private String netRatePerUnitRs;                 // Field 12 - Net Rate Per Unit (Rs)
    private String closingRatePerUnit;               // Field 13 - Closing Rate Per Unit (Only For derivatives)
    private String netTotalBeforeLevies;             // Field 14 - Net Total (Before Levies) (Rs.)
    private String remarks;                          // Field 15 - Remarks

    public NameClearingCorporationDto() {}

    public NameClearingCorporationDto(String uniqueId, String recordType, String orderNo, String orderTime,
                                      String tradeNo, String tradeTime, String securityContractDescription,
                                      String buySell, String quantity, String grossRatePricePerUnitForeignCcy,
                                      String grossRatePricePerUnitRs, String brokerageRs, String netRatePerUnitRs,
                                      String closingRatePerUnit, String netTotalBeforeLevies, String remarks) {
        this.uniqueId = uniqueId;
        this.recordType = recordType;
        this.orderNo = orderNo;
        this.orderTime = orderTime;
        this.tradeNo = tradeNo;
        this.tradeTime = tradeTime;
        this.securityContractDescription = securityContractDescription;
        this.buySell = buySell;
        this.quantity = quantity;
        this.grossRatePricePerUnitForeignCcy = grossRatePricePerUnitForeignCcy;
        this.grossRatePricePerUnitRs = grossRatePricePerUnitRs;
        this.brokerageRs = brokerageRs;
        this.netRatePerUnitRs = netRatePerUnitRs;
        this.closingRatePerUnit = closingRatePerUnit;
        this.netTotalBeforeLevies = netTotalBeforeLevies;
        this.remarks = remarks;
    }

    public String getUniqueId() { return uniqueId; }
    public void setUniqueId(String uniqueId) { this.uniqueId = uniqueId; }

    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public String getOrderTime() { return orderTime; }
    public void setOrderTime(String orderTime) { this.orderTime = orderTime; }

    public String getTradeNo() { return tradeNo; }
    public void setTradeNo(String tradeNo) { this.tradeNo = tradeNo; }

    public String getTradeTime() { return tradeTime; }
    public void setTradeTime(String tradeTime) { this.tradeTime = tradeTime; }

    public String getSecurityContractDescription() { return securityContractDescription; }
    public void setSecurityContractDescription(String securityContractDescription) { this.securityContractDescription = securityContractDescription; }

    public String getBuySell() { return buySell; }
    public void setBuySell(String buySell) { this.buySell = buySell; }

    public String getQuantity() { return quantity; }
    public void setQuantity(String quantity) { this.quantity = quantity; }

    public String getGrossRatePricePerUnitForeignCcy() { return grossRatePricePerUnitForeignCcy; }
    public void setGrossRatePricePerUnitForeignCcy(String grossRatePricePerUnitForeignCcy) { this.grossRatePricePerUnitForeignCcy = grossRatePricePerUnitForeignCcy; }

    public String getGrossRatePricePerUnitRs() { return grossRatePricePerUnitRs; }
    public void setGrossRatePricePerUnitRs(String grossRatePricePerUnitRs) { this.grossRatePricePerUnitRs = grossRatePricePerUnitRs; }

    public String getBrokerageRs() { return brokerageRs; }
    public void setBrokerageRs(String brokerageRs) { this.brokerageRs = brokerageRs; }

    public String getNetRatePerUnitRs() { return netRatePerUnitRs; }
    public void setNetRatePerUnitRs(String netRatePerUnitRs) { this.netRatePerUnitRs = netRatePerUnitRs; }

    public String getClosingRatePerUnit() { return closingRatePerUnit; }
    public void setClosingRatePerUnit(String closingRatePerUnit) { this.closingRatePerUnit = closingRatePerUnit; }

    public String getNetTotalBeforeLevies() { return netTotalBeforeLevies; }
    public void setNetTotalBeforeLevies(String netTotalBeforeLevies) { this.netTotalBeforeLevies = netTotalBeforeLevies; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    @Override
    public String toString() {
        return "DetailDto{" +
                "uniqueId='" + uniqueId + '\'' +
                ", recordType='" + recordType + '\'' +
                ", orderNo='" + orderNo + '\'' +
                ", orderTime='" + orderTime + '\'' +
                ", tradeNo='" + tradeNo + '\'' +
                ", tradeTime='" + tradeTime + '\'' +
                ", securityContractDescription='" + securityContractDescription + '\'' +
                ", buySell='" + buySell + '\'' +
                ", quantity='" + quantity + '\'' +
                ", grossRatePricePerUnitForeignCcy='" + grossRatePricePerUnitForeignCcy + '\'' +
                ", grossRatePricePerUnitRs='" + grossRatePricePerUnitRs + '\'' +
                ", brokerageRs='" + brokerageRs + '\'' +
                ", netRatePerUnitRs='" + netRatePerUnitRs + '\'' +
                ", closingRatePerUnit='" + closingRatePerUnit + '\'' +
                ", netTotalBeforeLevies='" + netTotalBeforeLevies + '\'' +
                ", remarks='" + remarks + '\'' +
                '}';
    }
}