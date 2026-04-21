package com.geojit.contractnotes.equity_combinemargin.v1.Model;

public class SCapitalHeaderTypeModel {

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

    public String getSegment() {
        return segment;
    }

    public void setSegment(String segment) {
        this.segment = segment;
    }

    public String getIsin() {
        return isin;
    }

    public void setIsin(String isin) {
        this.isin = isin;
    }

    public String getSecurityDescription() {
        return securityDescription;
    }

    public void setSecurityDescription(String securityDescription) {
        this.securityDescription = securityDescription;
    }

    public String getBuyQty() {
        return buyQty;
    }

    public void setBuyQty(String buyQty) {
        this.buyQty = buyQty;
    }

    public String getBuyWap() {
        return buyWap;
    }

    public void setBuyWap(String buyWap) {
        this.buyWap = buyWap;
    }

    public String getBuyBrokerage() {
        return buyBrokerage;
    }

    public void setBuyBrokerage(String buyBrokerage) {
        this.buyBrokerage = buyBrokerage;
    }

    public String getBuyWapAfterBrokerage() {
        return buyWapAfterBrokerage;
    }

    public void setBuyWapAfterBrokerage(String buyWapAfterBrokerage) {
        this.buyWapAfterBrokerage = buyWapAfterBrokerage;
    }

    public String getBuyValue() {
        return buyValue;
    }

    public void setBuyValue(String buyValue) {
        this.buyValue = buyValue;
    }

    public String getSellQty() {
        return sellQty;
    }

    public void setSellQty(String sellQty) {
        this.sellQty = sellQty;
    }

    public String getSellWap() {
        return sellWap;
    }

    public void setSellWap(String sellWap) {
        this.sellWap = sellWap;
    }

    public String getSellBrokerage() {
        return sellBrokerage;
    }

    public void setSellBrokerage(String sellBrokerage) {
        this.sellBrokerage = sellBrokerage;
    }

    public String getSellWapAfterBrokerage() {
        return sellWapAfterBrokerage;
    }

    public void setSellWapAfterBrokerage(String sellWapAfterBrokerage) {
        this.sellWapAfterBrokerage = sellWapAfterBrokerage;
    }

    public String getSellValue() {
        return sellValue;
    }

    public void setSellValue(String sellValue) {
        this.sellValue = sellValue;
    }

    public String getNetQty() {
        return netQty;
    }

    public void setNetQty(String netQty) {
        this.netQty = netQty;
    }

    public String getNetObligationIsin() {
        return netObligationIsin;
    }

    public void setNetObligationIsin(String netObligationIsin) {
        this.netObligationIsin = netObligationIsin;
    }

    private String partycode;
    private String headertype;          // S
    private String segment;             // CAPITAL
    private String isin;
    private String securityDescription;
    private String buyQty;
    private String buyWap;
    private String buyBrokerage;
    private String buyWapAfterBrokerage;
    private String buyValue;
    private String sellQty;
    private String sellWap;
    private String sellBrokerage;
    private String sellWapAfterBrokerage;
    private String sellValue;
    private String netQty;
    private String netObligationIsin;

    // getters & setters

    @Override
    public String toString() {
        return "SCapitalHeaderTypeModel [partycode=" + partycode +
                ", segment=" + segment +
                ", isin=" + isin +
                ", securityDescription=" + securityDescription +
                ", buyQty=" + buyQty +
                ", sellQty=" + sellQty +
                ", netQty=" + netQty +
                ", netObligationIsin=" + netObligationIsin + "]";
    }
}
