package com.geojit.contractnotes.equity_combinemargin.v1.Model;

public class DealingOfficeAddress {

    private String partycode;               // TRADE_CODE
    private String headertype;              // A
    private String dealingAddress;          // DEALING_OFFICE_ADDRESS
    private String gstLocation;             // GST_LOCATION
    private String dealingOfficeNo;         // DEALING_OFFICE_NO
    private String gstNo;                   // GSTIN

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

    public String getDealingAddress() {
        return dealingAddress;
    }

    public void setDealingAddress(String dealingAddress) {
        this.dealingAddress = dealingAddress;
    }

    public String getGstLocation() {
        return gstLocation;
    }

    public void setGstLocation(String gstLocation) {
        this.gstLocation = gstLocation;
    }

    public String getDealingOfficeNo() {
        return dealingOfficeNo;
    }

    public void setDealingOfficeNo(String dealingOfficeNo) {
        this.dealingOfficeNo = dealingOfficeNo;
    }

    public String getGstNo() {
        return gstNo;
    }

    public void setGstNo(String gstNo) {
        this.gstNo = gstNo;
    }

    @Override
    public String toString() {
        return "DealingOfficeAddress [partycode=" + partycode + ", headertype=" + headertype +
                ", dealingAddress=" + dealingAddress + ", gstLocation=" + gstLocation +
                ", dealingOfficeNo=" + dealingOfficeNo + ", gstNo=" + gstNo + "]";
    }
}