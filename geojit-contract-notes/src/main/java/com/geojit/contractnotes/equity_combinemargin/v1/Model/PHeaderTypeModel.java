package com.geojit.contractnotes.equity_combinemargin.v1.Model;

public class PHeaderTypeModel {

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

    public String getQty() {
        return qty;
    }

    public void setQty(String qty) {
        this.qty = qty;
    }

    public String getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(String totalValue) {
        this.totalValue = totalValue;
    }

    public String getHaircutValue() {
        return haircutValue;
    }

    public void setHaircutValue(String haircutValue) {
        this.haircutValue = haircutValue;
    }

    public String getBalanceAmount() {
        return balanceAmount;
    }

    public void setBalanceAmount(String balanceAmount) {
        this.balanceAmount = balanceAmount;
    }

    private String partycode;
    private String headertype;          // P
    private String securityDescription;
    private String qty;
    private String totalValue;
    private String haircutValue;
    private String balanceAmount;

    // getters & setters

    @Override
    public String toString() {
        return "PHeaderTypeModel [partycode=" + partycode +
                ", securityDescription=" + securityDescription +
                ", qty=" + qty +
                ", balanceAmount=" + balanceAmount + "]";
    }
}
