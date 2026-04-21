package com.geojit.contractnotes.equity_combinemargin.v1.Model;

public class OHeaderTypeModel {

    private String partycode;
    private String headertype;      // O
    private String segment;
    private String securityDescription;
    private String segment2;
    private String buyQty;
    private String buyRate;
    private String sellQty;
    private String sellRate;
    private String netQty;
    private String netRate;
    private String amount;
    private String stt;

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

    public String getSecurityDescription() {
        return securityDescription;
    }

    public void setSecurityDescription(String securityDescription) {
        this.securityDescription = securityDescription;
    }

    public String getSegment2() {
        return segment2;
    }

    public void setSegment2(String segment2) {
        this.segment2 = segment2;
    }

    public String getBuyQty() {
        return buyQty;
    }

    public void setBuyQty(String buyQty) {
        this.buyQty = buyQty;
    }

    public String getBuyRate() {
        return buyRate;
    }

    public void setBuyRate(String buyRate) {
        this.buyRate = buyRate;
    }

    public String getSellQty() {
        return sellQty;
    }

    public void setSellQty(String sellQty) {
        this.sellQty = sellQty;
    }

    public String getSellRate() {
        return sellRate;
    }

    public void setSellRate(String sellRate) {
        this.sellRate = sellRate;
    }

    public String getNetQty() {
        return netQty;
    }

    public void setNetQty(String netQty) {
        this.netQty = netQty;
    }

    public String getNetRate() {
        return netRate;
    }

    public void setNetRate(String netRate) {
        this.netRate = netRate;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getStt() {
        return stt;
    }

    public void setStt(String stt) {
        this.stt = stt;
    }
// getters & setters

    @Override
    public String toString() {
        return "OHeaderTypeModel [partycode=" + partycode +
                ", securityDescription=" + securityDescription +
                ", netQty=" + netQty +
                ", amount=" + amount + "]";
    }
}
