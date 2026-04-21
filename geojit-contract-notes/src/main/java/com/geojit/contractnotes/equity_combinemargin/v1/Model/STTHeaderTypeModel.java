package com.geojit.contractnotes.equity_combinemargin.v1.Model;

public class STTHeaderTypeModel {

    private String partycode;
    private String headertype;      // STT
    private String securityDescription;
    private String exchange;
    private String segment;
    private String expDate;
    private String futureSale;
    private String futureStt;
    private String optionSale;
    private String optionStt;
    private String totalStt;

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

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getSegment() {
        return segment;
    }

    public void setSegment(String segment) {
        this.segment = segment;
    }

    public String getExpDate() {
        return expDate;
    }

    public void setExpDate(String expDate) {
        this.expDate = expDate;
    }

    public String getFutureSale() {
        return futureSale;
    }

    public void setFutureSale(String futureSale) {
        this.futureSale = futureSale;
    }

    public String getFutureStt() {
        return futureStt;
    }

    public void setFutureStt(String futureStt) {
        this.futureStt = futureStt;
    }

    public String getOptionSale() {
        return optionSale;
    }

    public void setOptionSale(String optionSale) {
        this.optionSale = optionSale;
    }

    public String getOptionStt() {
        return optionStt;
    }

    public void setOptionStt(String optionStt) {
        this.optionStt = optionStt;
    }

    public String getTotalStt() {
        return totalStt;
    }

    public void setTotalStt(String totalStt) {
        this.totalStt = totalStt;
    }
// getters & setters

    @Override
    public String toString() {
        return "STTHeaderTypeModel [partycode=" + partycode +
                ", securityDescription=" + securityDescription +
                ", totalStt=" + totalStt + "]";
    }
}
