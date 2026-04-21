package com.geojit.contractnotes.equity_combinemargin.v1.Model;

public class CHeaderTypeModel {

    private String partycode;           // PLV083
    private String headertype;          // C
    private String chargeType;          // SEBI/MTM
    private String date;                // 03.07.2025
    private String segment;
    private String scrip_name;
    private String buy_qty;
    private String buy_market_rate;
    private String sell_qty;
    private String sell_market_rate;
    private String brokerage;
    private String stt;
    private String other_charges;
    private String grand_total;

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

    public String getScrip_name() {
        return scrip_name;
    }

    public void setScrip_name(String scrip_name) {
        this.scrip_name = scrip_name;
    }

    public String getBuy_qty() {
        return buy_qty;
    }

    public void setBuy_qty(String buy_qty) {
        this.buy_qty = buy_qty;
    }

    public String getBuy_market_rate() {
        return buy_market_rate;
    }

    public void setBuy_market_rate(String buy_market_rate) {
        this.buy_market_rate = buy_market_rate;
    }

    public String getSell_qty() {
        return sell_qty;
    }

    public void setSell_qty(String sell_qty) {
        this.sell_qty = sell_qty;
    }

    public String getSell_market_rate() {
        return sell_market_rate;
    }

    public void setSell_market_rate(String sell_market_rate) {
        this.sell_market_rate = sell_market_rate;
    }

    public String getBrokerage() {
        return brokerage;
    }

    public void setBrokerage(String brokerage) {
        this.brokerage = brokerage;
    }

    public String getStt() {
        return stt;
    }

    public void setStt(String stt) {
        this.stt = stt;
    }

    public String getOther_charges() {
        return other_charges;
    }

    public void setOther_charges(String other_charges) {
        this.other_charges = other_charges;
    }

    public String getGrand_total() {
        return grand_total;
    }

    public void setGrand_total(String grand_total) {
        this.grand_total = grand_total;
    }

    @Override
    public String toString() {
        return "CAHeaderTypeModel [partycode=" + partycode +
                ", headertype=" + headertype +
                ", segment=" + segment +
                ", scrip_name=" + scrip_name +
                ", buy_qty=" + buy_qty +
                ", buy_market_rate=" + buy_market_rate +
                ", sell_qty=" + sell_qty +
                ", sell_market_rate=" + sell_market_rate +
                ", brokerage=" + brokerage +
                ", stt=" + stt +
                ", other_charges=" + other_charges +
                ", grand_total=" + grand_total + "]";
    }
}
