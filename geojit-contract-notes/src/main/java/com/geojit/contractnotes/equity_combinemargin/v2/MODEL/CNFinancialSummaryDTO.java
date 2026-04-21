package com.geojit.contractnotes.equity_combinemargin.v2.MODEL;

public class CNFinancialSummaryDTO {
	
	String tradeCode;
	String recordType; // F
	String exchange;
	String segment;
	String instrument;
	String payInOutObligation;
	String securityTxnTax;
	String sgst;
	String cgst;
	String igst;
	String tds;
	String exchangeTxnCharges;
	String sebiFee;
	String addCess;
	String stampDuty;
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
	public String getInstrument() {
		return instrument;
	}
	public void setInstrument(String instrument) {
		this.instrument = instrument;
	}
	public String getPayInOutObligation() {
		return payInOutObligation;
	}
	public void setPayInOutObligation(String payInOutObligation) {
		this.payInOutObligation = payInOutObligation;
	}
	public String getSecurityTxnTax() {
		return securityTxnTax;
	}
	public void setSecurityTxnTax(String securityTxnTax) {
		this.securityTxnTax = securityTxnTax;
	}
	public String getSgst() {
		return sgst;
	}
	public void setSgst(String sgst) {
		this.sgst = sgst;
	}
	public String getCgst() {
		return cgst;
	}
	public void setCgst(String cgst) {
		this.cgst = cgst;
	}
	public String getIgst() {
		return igst;
	}
	public void setIgst(String igst) {
		this.igst = igst;
	}
	public String getTds() {
		return tds;
	}
	public void setTds(String tds) {
		this.tds = tds;
	}
	public String getExchangeTxnCharges() {
		return exchangeTxnCharges;
	}
	public void setExchangeTxnCharges(String exchangeTxnCharges) {
		this.exchangeTxnCharges = exchangeTxnCharges;
	}
	public String getSebiFee() {
		return sebiFee;
	}
	public void setSebiFee(String sebiFee) {
		this.sebiFee = sebiFee;
	}
	public String getAddCess() {
		return addCess;
	}
	public void setAddCess(String addCess) {
		this.addCess = addCess;
	}
	public String getStampDuty() {
		return stampDuty;
	}
	public void setStampDuty(String stampDuty) {
		this.stampDuty = stampDuty;
	}
	public String getNetAmount() {
		return netAmount;
	}
	public void setNetAmount(String netAmount) {
		this.netAmount = netAmount;
	}
	
	@Override
	public String toString() {
		return "CNFinancialSummaryDTO [tradeCode=" + tradeCode + ", recordType=" + recordType + ", exchange="
				+ exchange + ", segment=" + segment + ", instrument=" + instrument + ", payInOutObligation="
				+ payInOutObligation + ", securityTxnTax=" + securityTxnTax + ", sgst=" + sgst + ", cgst=" + cgst
				+ ", igst=" + igst + ", tds=" + tds + ", exchangeTxnCharges=" + exchangeTxnCharges + ", sebiFee="
				+ sebiFee + ", addCess=" + addCess + ", stampDuty=" + stampDuty + ", netAmount=" + netAmount + "]";
	}
}