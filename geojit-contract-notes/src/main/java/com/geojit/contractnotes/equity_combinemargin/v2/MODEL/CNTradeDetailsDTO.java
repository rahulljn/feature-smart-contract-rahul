package com.geojit.contractnotes.equity_combinemargin.v2.MODEL;

public class CNTradeDetailsDTO {
	
	String tradeCode;
	String recordType; // D
	String exchange;
	String segment;
	String orderNo;
	String orderTime;
	String tradeNo;
	String tradeTime;
	String contractDesc;
	String tradeType;
	String orderQty;
	String grossRateFc;
	String grossRate;
	String brokerage;
	String netRatePerUnit;
	String orderClosingRate;
	String netTotal;
	String remarks;
	String subtotalDesc;
	String sttlNo;
	String sttlDate;
	String segmentType;
	String brokerCode;
	
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
	public String getOrderNo() {
		return orderNo;
	}
	public void setOrderNo(String orderNo) {
		this.orderNo = orderNo;
	}
	public String getOrderTime() {
		return orderTime;
	}
	public void setOrderTime(String orderTime) {
		this.orderTime = orderTime;
	}
	public String getTradeNo() {
		return tradeNo;
	}
	public void setTradeNo(String tradeNo) {
		this.tradeNo = tradeNo;
	}
	public String getTradeTime() {
		return tradeTime;
	}
	public void setTradeTime(String tradeTime) {
		this.tradeTime = tradeTime;
	}
	public String getContractDesc() {
		return contractDesc;
	}
	public void setContractDesc(String contractDesc) {
		this.contractDesc = contractDesc;
	}
	public String getTradeType() {
		return tradeType;
	}
	public void setTradeType(String tradeType) {
		this.tradeType = tradeType;
	}
	public String getOrderQty() {
		return orderQty;
	}
	public void setOrderQty(String orderQty) {
		this.orderQty = orderQty;
	}
	public String getGrossRateFc() {
		return grossRateFc;
	}
	public void setGrossRateFc(String grossRateFc) {
		this.grossRateFc = grossRateFc;
	}
	public String getGrossRate() {
		return grossRate;
	}
	public void setGrossRate(String grossRate) {
		this.grossRate = grossRate;
	}
	public String getBrokerage() {
		return brokerage;
	}
	public void setBrokerage(String brokerage) {
		this.brokerage = brokerage;
	}
	public String getNetRatePerUnit() {
		return netRatePerUnit;
	}
	public void setNetRatePerUnit(String netRatePerUnit) {
		this.netRatePerUnit = netRatePerUnit;
	}
	public String getOrderClosingRate() {
		return orderClosingRate;
	}
	public void setOrderClosingRate(String orderClosingRate) {
		this.orderClosingRate = orderClosingRate;
	}
	public String getNetTotal() {
		return netTotal;
	}
	public void setNetTotal(String netTotal) {
		this.netTotal = netTotal;
	}
	public String getRemarks() {
		return remarks;
	}
	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}
	public String getSubtotalDesc() {
		return subtotalDesc;
	}
	public void setSubtotalDesc(String subtotalDesc) {
		this.subtotalDesc = subtotalDesc;
	}
	public String getSttlNo() {
		return sttlNo;
	}
	public void setSttlNo(String sttlNo) {
		this.sttlNo = sttlNo;
	}
	public String getSttlDate() {
		return sttlDate;
	}
	public void setSttlDate(String sttlDate) {
		this.sttlDate = sttlDate;
	}
	public String getSegmentType() {
		return segmentType;
	}
	public void setSegmentType(String segmentType) {
		this.segmentType = segmentType;
	}
	public String getBrokerCode() {
		return brokerCode;
	}
	public void setBrokerCode(String brokerCode) {
		this.brokerCode = brokerCode;
	}
	
	@Override
	public String toString() {
		return "CNTradeDetailsDTO [tradeCode=" + tradeCode + ", recordType=" + recordType + ", exchange=" + exchange
				+ ", segment=" + segment + ", orderNo=" + orderNo + ", orderTime=" + orderTime + ", tradeNo="
				+ tradeNo + ", tradeTime=" + tradeTime + ", contractDesc=" + contractDesc + ", tradeType=" + tradeType
				+ ", orderQty=" + orderQty + ", grossRateFc=" + grossRateFc + ", grossRate=" + grossRate
				+ ", brokerage=" + brokerage + ", netRatePerUnit=" + netRatePerUnit + ", orderClosingRate="
				+ orderClosingRate + ", netTotal=" + netTotal + ", remarks=" + remarks + ", subtotalDesc="
				+ subtotalDesc + ", sttlNo=" + sttlNo + ", sttlDate=" + sttlDate + ", segmentType=" + segmentType
				+ ", brokerCode=" + brokerCode + "]";
	}
}