package com.geojit.contractnotes.equity_combinemargin.v2.MODEL;

public class CNScripSummaryDTO {
	
	String tradeCode;
	String recordType; // SS
	String securityDescription;
	String tradeType;
	String tradeQty;
	String grossRate;
	String grossTotal;
	String grossBrokerage;
	String brokerage;
	String netRate;
	String netAmount;
	
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
	
	@Override
	public String toString() {
		return "CNScripSummaryDTO [tradeCode=" + tradeCode + ", recordType=" + recordType + ", securityDescription="
				+ securityDescription + ", tradeType=" + tradeType + ", tradeQty=" + tradeQty + ", grossRate="
				+ grossRate + ", grossTotal=" + grossTotal + ", grossBrokerage=" + grossBrokerage + ", brokerage="
				+ brokerage + ", netRate=" + netRate + ", netAmount=" + netAmount + "]";
	}
}