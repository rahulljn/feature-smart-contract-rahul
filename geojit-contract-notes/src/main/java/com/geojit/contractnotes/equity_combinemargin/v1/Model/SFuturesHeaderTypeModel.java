package com.geojit.contractnotes.equity_combinemargin.v1.Model;

public class SFuturesHeaderTypeModel {

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

    public String getContractDesc() {
        return contractDesc;
    }

    public void setContractDesc(String contractDesc) {
        this.contractDesc = contractDesc;
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

    public String getTradeWapFc() {
        return tradeWapFc;
    }

    public void setTradeWapFc(String tradeWapFc) {
        this.tradeWapFc = tradeWapFc;
    }

    public String getTradeWap() {
        return tradeWap;
    }

    public void setTradeWap(String tradeWap) {
        this.tradeWap = tradeWap;
    }

    public String getTradeBrokerage() {
        return tradeBrokerage;
    }

    public void setTradeBrokerage(String tradeBrokerage) {
        this.tradeBrokerage = tradeBrokerage;
    }

    public String getTradeWapAfterBrokerage() {
        return tradeWapAfterBrokerage;
    }

    public void setTradeWapAfterBrokerage(String tradeWapAfterBrokerage) {
        this.tradeWapAfterBrokerage = tradeWapAfterBrokerage;
    }

    public String getTradeClosingRate() {
        return tradeClosingRate;
    }

    public void setTradeClosingRate(String tradeClosingRate) {
        this.tradeClosingRate = tradeClosingRate;
    }

    public String getTradeNetTotal() {
        return tradeNetTotal;
    }

    public void setTradeNetTotal(String tradeNetTotal) {
        this.tradeNetTotal = tradeNetTotal;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    private String partycode;
    private String headertype;      // S
    private String segment;         // FUTURES
    private String contractDesc;
    private String tradeType;       // B / S
    private String tradeQty;
    private String tradeWapFc;
    private String tradeWap;
    private String tradeBrokerage;
    private String tradeWapAfterBrokerage;
    private String tradeClosingRate;
    private String tradeNetTotal;
    private String remarks;

    // getters & setters

    @Override
    public String toString() {
        return "SFuturesHeaderTypeModel [partycode=" + partycode +
                ", segment=" + segment +
                ", contractDesc=" + contractDesc +
                ", tradeType=" + tradeType +
                ", tradeQty=" + tradeQty +
                ", tradeNetTotal=" + tradeNetTotal + "]";
    }
}
