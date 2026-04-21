package com.geojit.contractnotes.equity_combinemargin.v1.Model;

public class FOHeaderTypeModel {

    private String partycode;           // PLV083
    private String headertype;          // F
    private String exchange;            // BSE/NSE
    private String segment;             // FO/EN/CASH/CDS
    private String product;             // OPT/blank
    private String turnover;            // 308
    private String noOfTrades;          // 8
    private String totalBrokerage;      // 4.04
    private String netBrokerage;        // 4.03
    private String totalStampDuty;      // 0
    private String netStampDuty;        // 0.00
    private String totalStampCharges;   // 4.8264
    private String netStampCharges;     // 0.0126
    private String totalGst;            // 0
    private String netGst;              // 0
    private String netAmountPayableReceivable; // 287.09
    private String brokeragePerUnit;
    public String getSecurityname() {
        return securityname;
    }

    public void setSecurityname(String securityname) {
        this.securityname = securityname;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getBuySell() {
        return buySell;
    }

    public void setBuySell(String buySell) {
        this.buySell = buySell;
    }

    public String getQuantity() {
        return quantity;
    }

    public String getBrokeragePerUnit() {
        return brokeragePerUnit;
    }

    public void setBrokeragePerUnit(String brokeragePerUnit) {
        this.brokeragePerUnit = brokeragePerUnit;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getBrokerage() {
        return brokerage;
    }

    public void setBrokerage(String brokerage) {
        this.brokerage = brokerage;
    }

    public String getClosingRate() {
        return closingRate;
    }

    public void setClosingRate(String closingRate) {
        this.closingRate = closingRate;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    private String securityname;
    private String symbol;
    private String buySell;
    private  String quantity;
    private String price;
    private String brokerage;
    private String closingRate;
    private  String remarks;


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

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getTurnover() {
        return turnover;
    }

    public void setTurnover(String turnover) {
        this.turnover = turnover;
    }

    public String getNoOfTrades() {
        return noOfTrades;
    }

    public void setNoOfTrades(String noOfTrades) {
        this.noOfTrades = noOfTrades;
    }

    public String getTotalBrokerage() {
        return totalBrokerage;
    }

    public void setTotalBrokerage(String totalBrokerage) {
        this.totalBrokerage = totalBrokerage;
    }

    public String getNetBrokerage() {
        return netBrokerage;
    }

    public void setNetBrokerage(String netBrokerage) {
        this.netBrokerage = netBrokerage;
    }

    public String getTotalStampDuty() {
        return totalStampDuty;
    }

    public void setTotalStampDuty(String totalStampDuty) {
        this.totalStampDuty = totalStampDuty;
    }

    public String getNetStampDuty() {
        return netStampDuty;
    }

    public void setNetStampDuty(String netStampDuty) {
        this.netStampDuty = netStampDuty;
    }

    public String getTotalStampCharges() {
        return totalStampCharges;
    }

    public void setTotalStampCharges(String totalStampCharges) {
        this.totalStampCharges = totalStampCharges;
    }

    public String getNetStampCharges() {
        return netStampCharges;
    }

    public void setNetStampCharges(String netStampCharges) {
        this.netStampCharges = netStampCharges;
    }

    public String getTotalGst() {
        return totalGst;
    }

    public void setTotalGst(String totalGst) {
        this.totalGst = totalGst;
    }

    public String getNetGst() {
        return netGst;
    }

    public void setNetGst(String netGst) {
        this.netGst = netGst;
    }

    public String getNetAmountPayableReceivable() {
        return netAmountPayableReceivable;
    }

    public void setNetAmountPayableReceivable(String netAmountPayableReceivable) {
        this.netAmountPayableReceivable = netAmountPayableReceivable;
    }
//    public String getSecurityname() {
//        return securityname;
//    }
//
//    public String getSymbol() {
//        return symbol;
//    }
//
//    public String getBuySell() {
//        return buySell;
//    }
//
//    public String getQuantity() {
//        return quantity;
//    }
//
//    public String getPrice() {
//        return price;
//    }
//
//    public String getBrokerage() {
//        return brokerage;
//    }
//
//    public String getClosingRate() {
//        return closingRate;
//    }
//
//    public String getRemarks() {
//        return remarks;
//    }
    @Override
    public String toString() {
        return "FOHeaderTypeModel [partycode=" + partycode +
                ", headertype=" + headertype +
                ", exchange=" + exchange +
                ", segment=" + segment +
                ", product=" + product +
                ", turnover=" + turnover +
                ", noOfTrades=" + noOfTrades +
                ", totalBrokerage=" + totalBrokerage +
                ", netBrokerage=" + netBrokerage +
                ", totalStampDuty=" + totalStampDuty +
                ", netStampDuty=" + netStampDuty +
                ", totalStampCharges=" + totalStampCharges +
                ", netStampCharges=" + netStampCharges +
                ", totalGst=" + totalGst +
                ", netGst=" + netGst +
                ", netAmountPayableReceivable=" + netAmountPayableReceivable + "]";
    }

    public void setNetTotal(String safe) {
    }
}