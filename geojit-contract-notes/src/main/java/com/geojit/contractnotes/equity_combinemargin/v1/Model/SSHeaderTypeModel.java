package com.geojit.contractnotes.equity_combinemargin.v1.Model;

public class SSHeaderTypeModel {

    private String partycode;
    private String headertype;          // SS
    private String securityDescription;
    private String tradeType;
    private String tradeQty;
    private String grossRate;
    private String grossTotal;
    private String grossBrokerage;
    private String brokerage;
    private String netRate;
    private String netAmount;

    public String getPartycode() {
        return partycode;
    }

    public void setPartycode(String partycode) {
        this.partycode = partycode;
    }

    public String getHeadertype() {
        return headertype;
    }

    public void setHeadertype(String headertype) {
        this.headertype = headertype;
    }

    public String getSecurityDescription() {
        return securityDescription;
    }

    public void setSecurityDescription(String securityDescription) {
        this.securityDescription = securityDescription;
    }

    public String getTradeType() {
        return tradeType;
    }

    public void setTradeType(String tradeType) {
        this.tradeType = tradeType;
    }

    public String getTradeQty() {
        return tradeQty;
    }

    public void setTradeQty(String tradeQty) {
        this.tradeQty = tradeQty;
    }

    public String getGrossRate() {
        return grossRate;
    }

    public void setGrossRate(String grossRate) {
        this.grossRate = grossRate;
    }

    public String getGrossTotal() {
        return grossTotal;
    }

    public void setGrossTotal(String grossTotal) {
        this.grossTotal = grossTotal;
    }

    public String getGrossBrokerage() {
        return grossBrokerage;
    }

    public void setGrossBrokerage(String grossBrokerage) {
        this.grossBrokerage = grossBrokerage;
    }

    public String getBrokerage() {
        return brokerage;
    }

    public void setBrokerage(String brokerage) {
        this.brokerage = brokerage;
    }

    public String getNetRate() {
        return netRate;
    }

    public void setNetRate(String netRate) {
        this.netRate = netRate;
    }

    public String getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(String netAmount) {
        this.netAmount = netAmount;
    }
// getters & setters

    @Override
    public String toString() {
        return "SSHeaderTypeModel [partycode=" + partycode +
                ", securityDescription=" + securityDescription +
                ", tradeQty=" + tradeQty +
                ", netAmount=" + netAmount + "]";
    }
}
