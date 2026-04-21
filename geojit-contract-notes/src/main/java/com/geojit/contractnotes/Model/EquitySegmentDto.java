package com.geojit.contractnotes.Model;


public class EquitySegmentDto {

//    <Unique Id>~ P~ <ISIN>~ <Security Name/Symbol>~
//    <Quantity>~ <WAP>~ <Brokerage Per Share>~
//    <WAP(across exchanges after brokerage (Rs))>~
//    <Total Buy>~ <Quantity>~ <WAP>~ <Brokerage Per Share>~
//    <WAP(across exchanges after brokerage (Rs))>~
//    <Total Sell Value after Brokerage>~ <Net Quantity>~ <Net Obligation>
    private String uniqueId;                          // Field 0
    private String recordType;                        // Field 1 - Always "P"
    private String isin;                              // Field 2
    private String securityNameSymbol;                // Field 3
    private String buyQuantity;                       // Field 4
    private String buyWAP;                            // Field 5
    private String buyBrokeragePerShare;              // Field 6
    private String buyWAPAfterBrokerage;              // Field 7
    private String totalBuy;                          // Field 8
    private String sellQuantity;                      // Field 9
    private String sellWAP;                           // Field 10
    private String sellBrokeragePerShare;             // Field 11
    private String sellWAPAfterBrokerage;             // Field 12
    private String totalSellValueAfterBrokerage;      // Field 13
    private String netQuantity;                       // Field 14
    private String netObligation;                     // Field 15 - Note: This would be index 15 if count is 16

    // Constructors
    public EquitySegmentDto() {
    }

    public EquitySegmentDto(String uniqueId, String recordType, String isin, String securityNameSymbol,
                            String buyQuantity, String buyWAP, String buyBrokeragePerShare,
                            String buyWAPAfterBrokerage, String totalBuy, String sellQuantity,
                            String sellWAP, String sellBrokeragePerShare, String sellWAPAfterBrokerage,
                            String totalSellValueAfterBrokerage, String netQuantity, String netObligation) {
        this.uniqueId = uniqueId;
        this.recordType = recordType;
        this.isin = isin;
        this.securityNameSymbol = securityNameSymbol;
        this.buyQuantity = buyQuantity;
        this.buyWAP = buyWAP;
        this.buyBrokeragePerShare = buyBrokeragePerShare;
        this.buyWAPAfterBrokerage = buyWAPAfterBrokerage;
        this.totalBuy = totalBuy;
        this.sellQuantity = sellQuantity;
        this.sellWAP = sellWAP;
        this.sellBrokeragePerShare = sellBrokeragePerShare;
        this.sellWAPAfterBrokerage = sellWAPAfterBrokerage;
        this.totalSellValueAfterBrokerage = totalSellValueAfterBrokerage;
        this.netQuantity = netQuantity;
        this.netObligation = netObligation;
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

    public String getIsin() {
        return isin;
    }

    public void setIsin(String isin) {
        this.isin = isin;
    }

    public String getSecurityNameSymbol() {
        return securityNameSymbol;
    }

    public void setSecurityNameSymbol(String securityNameSymbol) {
        this.securityNameSymbol = securityNameSymbol;
    }

    public String getBuyQuantity() {
        return buyQuantity;
    }

    public void setBuyQuantity(String buyQuantity) {
        this.buyQuantity = buyQuantity;
    }

    public String getBuyWAP() {
        return buyWAP;
    }

    public void setBuyWAP(String buyWAP) {
        this.buyWAP = buyWAP;
    }

    public String getBuyBrokeragePerShare() {
        return buyBrokeragePerShare;
    }

    public void setBuyBrokeragePerShare(String buyBrokeragePerShare) {
        this.buyBrokeragePerShare = buyBrokeragePerShare;
    }

    public String getBuyWAPAfterBrokerage() {
        return buyWAPAfterBrokerage;
    }

    public void setBuyWAPAfterBrokerage(String buyWAPAfterBrokerage) {
        this.buyWAPAfterBrokerage = buyWAPAfterBrokerage;
    }

    public String getTotalBuy() {
        return totalBuy;
    }

    public void setTotalBuy(String totalBuy) {
        this.totalBuy = totalBuy;
    }

    public String getSellQuantity() {
        return sellQuantity;
    }

    public void setSellQuantity(String sellQuantity) {
        this.sellQuantity = sellQuantity;
    }

    public String getSellWAP() {
        return sellWAP;
    }

    public void setSellWAP(String sellWAP) {
        this.sellWAP = sellWAP;
    }

    public String getSellBrokeragePerShare() {
        return sellBrokeragePerShare;
    }

    public void setSellBrokeragePerShare(String sellBrokeragePerShare) {
        this.sellBrokeragePerShare = sellBrokeragePerShare;
    }

    public String getSellWAPAfterBrokerage() {
        return sellWAPAfterBrokerage;
    }

    public void setSellWAPAfterBrokerage(String sellWAPAfterBrokerage) {
        this.sellWAPAfterBrokerage = sellWAPAfterBrokerage;
    }

    public String getTotalSellValueAfterBrokerage() {
        return totalSellValueAfterBrokerage;
    }

    public void setTotalSellValueAfterBrokerage(String totalSellValueAfterBrokerage) {
        this.totalSellValueAfterBrokerage = totalSellValueAfterBrokerage;
    }

    public String getNetQuantity() {
        return netQuantity;
    }

    public void setNetQuantity(String netQuantity) {
        this.netQuantity = netQuantity;
    }

    public String getNetObligation() {
        return netObligation;
    }

    public void setNetObligation(String netObligation) {
        this.netObligation = netObligation;
    }

    @Override
    public String toString() {
        return "PositionDto{" +
                "uniqueId='" + uniqueId + '\'' +
                ", recordType='" + recordType + '\'' +
                ", isin='" + isin + '\'' +
                ", securityNameSymbol='" + securityNameSymbol + '\'' +
                ", buyQuantity='" + buyQuantity + '\'' +
                ", buyWAP='" + buyWAP + '\'' +
                ", buyBrokeragePerShare='" + buyBrokeragePerShare + '\'' +
                ", buyWAPAfterBrokerage='" + buyWAPAfterBrokerage + '\'' +
                ", totalBuy='" + totalBuy + '\'' +
                ", sellQuantity='" + sellQuantity + '\'' +
                ", sellWAP='" + sellWAP + '\'' +
                ", sellBrokeragePerShare='" + sellBrokeragePerShare + '\'' +
                ", sellWAPAfterBrokerage='" + sellWAPAfterBrokerage + '\'' +
                ", totalSellValueAfterBrokerage='" + totalSellValueAfterBrokerage + '\'' +
                ", netQuantity='" + netQuantity + '\'' +
                ", netObligation='" + netObligation + '\'' +
                '}';
    }
}