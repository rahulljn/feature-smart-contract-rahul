package com.geojit.contractnotes.equity_combinemargin.v2.MODEL;

public class CNPledgeSecuritiesDTO {
	
	String tradeCode;
	String recordType; // P
	String securityDescription;
	String qty;
	String totalValue;
	String haircutValue;
	String balanceAmount;
	
	public String getTradeCode() {
		return tradeCode;
	}
	public void setTradeCode(String tradeCode) {
		this.tradeCode = tradeCode;
	}
	public String getRecordType() {
		return recordType;
	}
	public void setRecordType(String recordType) {
		this.recordType = recordType;
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
	
	@Override
	public String toString() {
		return "CNPledgeSecuritiesDTO [tradeCode=" + tradeCode + ", recordType=" + recordType
				+ ", securityDescription=" + securityDescription + ", qty=" + qty + ", totalValue=" + totalValue
				+ ", haircutValue=" + haircutValue + ", balanceAmount=" + balanceAmount + "]";
	}
}