package com.geojit.contractnotes.equity_combinemargin.v2.MODEL;

public class CNMarginStatementDTO {
	
	String tradeCode;
	String recordType; // M
	String exchange;
	String segment;
	String tradeDate;
	String funds;
	String securityValue;
	String marginPledgeSecurityValue;
	String fdr;
	String approvedMargin;
	String availableMargin;
	String upfrontMargin;
	String mtm;
	String deliveryMargin;
	String requirement;
	String cc;
	String addMargin;
	String marginStatus;
	
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
	public String getTradeDate() {
		return tradeDate;
	}
	public void setTradeDate(String tradeDate) {
		this.tradeDate = tradeDate;
	}
	public String getFunds() {
		return funds;
	}
	public void setFunds(String funds) {
		this.funds = funds;
	}
	public String getSecurityValue() {
		return securityValue;
	}
	public void setSecurityValue(String securityValue) {
		this.securityValue = securityValue;
	}
	public String getMarginPledgeSecurityValue() {
		return marginPledgeSecurityValue;
	}
	public void setMarginPledgeSecurityValue(String marginPledgeSecurityValue) {
		this.marginPledgeSecurityValue = marginPledgeSecurityValue;
	}
	public String getFdr() {
		return fdr;
	}
	public void setFdr(String fdr) {
		this.fdr = fdr;
	}
	public String getApprovedMargin() {
		return approvedMargin;
	}
	public void setApprovedMargin(String approvedMargin) {
		this.approvedMargin = approvedMargin;
	}
	public String getAvailableMargin() {
		return availableMargin;
	}
	public void setAvailableMargin(String availableMargin) {
		this.availableMargin = availableMargin;
	}
	public String getUpfrontMargin() {
		return upfrontMargin;
	}
	public void setUpfrontMargin(String upfrontMargin) {
		this.upfrontMargin = upfrontMargin;
	}
	public String getMtm() {
		return mtm;
	}
	public void setMtm(String mtm) {
		this.mtm = mtm;
	}
	public String getDeliveryMargin() {
		return deliveryMargin;
	}
	public void setDeliveryMargin(String deliveryMargin) {
		this.deliveryMargin = deliveryMargin;
	}
	public String getRequirement() {
		return requirement;
	}
	public void setRequirement(String requirement) {
		this.requirement = requirement;
	}
	public String getCc() {
		return cc;
	}
	public void setCc(String cc) {
		this.cc = cc;
	}
	public String getAddMargin() {
		return addMargin;
	}
	public void setAddMargin(String addMargin) {
		this.addMargin = addMargin;
	}
	public String getMarginStatus() {
		return marginStatus;
	}
	public void setMarginStatus(String marginStatus) {
		this.marginStatus = marginStatus;
	}
	
	@Override
	public String toString() {
		return "CNMarginStatementDTO [tradeCode=" + tradeCode + ", recordType=" + recordType + ", exchange=" + exchange
				+ ", segment=" + segment + ", tradeDate=" + tradeDate + ", funds=" + funds + ", securityValue="
				+ securityValue + ", marginPledgeSecurityValue=" + marginPledgeSecurityValue + ", fdr=" + fdr
				+ ", approvedMargin=" + approvedMargin + ", availableMargin=" + availableMargin + ", upfrontMargin="
				+ upfrontMargin + ", mtm=" + mtm + ", deliveryMargin=" + deliveryMargin + ", requirement="
				+ requirement + ", cc=" + cc + ", addMargin=" + addMargin + ", marginStatus=" + marginStatus + "]";
	}
}