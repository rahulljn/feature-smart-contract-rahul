package com.geojit.contractnotes.equity_combinemargin.v2.MODEL;

public class CNNetObligationDTO {
	
	String tradeCode;
	String recordType; // O
	String segment;
	String securityDescription;
	String segmentType;
	String buyQty;
	String buyRate;
	String sellQty;
	String sellRate;
	String netQty;
	String netRate;
	String amount;
	String securityTxnTax;
	
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
	public String getSegmentType() {
		return segmentType;
	}
	public void setSegmentType(String segmentType) {
		this.segmentType = segmentType;
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
	public String getSecurityTxnTax() {
		return securityTxnTax;
	}
	public void setSecurityTxnTax(String securityTxnTax) {
		this.securityTxnTax = securityTxnTax;
	}
	
	@Override
	public String toString() {
		return "CNNetObligationDTO [tradeCode=" + tradeCode + ", recordType=" + recordType + ", segment=" + segment
				+ ", securityDescription=" + securityDescription + ", segmentType=" + segmentType + ", buyQty="
				+ buyQty + ", buyRate=" + buyRate + ", sellQty=" + sellQty + ", sellRate=" + sellRate + ", netQty="
				+ netQty + ", netRate=" + netRate + ", amount=" + amount + ", securityTxnTax=" + securityTxnTax + "]";
	}
}