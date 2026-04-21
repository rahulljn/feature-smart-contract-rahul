package com.geojit.contractnotes.equity_combinemargin.v2.MODEL;

public class CNSecuritySummaryDTO {
	
	String tradeCode;
	String recordType; // S
	String segment;
	
	// For Equity type (ISIN-based)
	String isin;
	String securityDescription;
	String buyQty;
	String buyWap;
	String buyBrokerage;
	String buyWapAfterBrokerage;
	String buyValue;
	String sellQty;
	String sellWap;
	String sellBrokerage;
	String sellWapAfterBrokerage;
	String sellValue;
	String netQty;
	String netObligationIsin;
	
	// For Derivative type (Contract-based)
	String contractDesc;
	String tradeType;
	String tradeQty;
	String tradeWapFc;
	String tradeWap;
	String tradeBrokerage;
	String tradeWapAfterBrokerage;
	String tradeClosingRate;
	String tradeNetTotal;
	String remarks;
	
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
	public String getIsin() {
		return isin;
	}
	public void setIsin(String isin) {
		this.isin = isin;
	}
	public String getSecurityDescription() {
		return securityDescription;
	}
	public void setSecurityDescription(String securityDescription) {
		this.securityDescription = securityDescription;
	}
	public String getBuyQty() {
		return buyQty;
	}
	public void setBuyQty(String buyQty) {
		this.buyQty = buyQty;
	}
	public String getBuyWap() {
		return buyWap;
	}
	public void setBuyWap(String buyWap) {
		this.buyWap = buyWap;
	}
	public String getBuyBrokerage() {
		return buyBrokerage;
	}
	public void setBuyBrokerage(String buyBrokerage) {
		this.buyBrokerage = buyBrokerage;
	}
	public String getBuyWapAfterBrokerage() {
		return buyWapAfterBrokerage;
	}
	public void setBuyWapAfterBrokerage(String buyWapAfterBrokerage) {
		this.buyWapAfterBrokerage = buyWapAfterBrokerage;
	}
	public String getBuyValue() {
		return buyValue;
	}
	public void setBuyValue(String buyValue) {
		this.buyValue = buyValue;
	}
	public String getSellQty() {
		return sellQty;
	}
	public void setSellQty(String sellQty) {
		this.sellQty = sellQty;
	}
	public String getSellWap() {
		return sellWap;
	}
	public void setSellWap(String sellWap) {
		this.sellWap = sellWap;
	}
	public String getSellBrokerage() {
		return sellBrokerage;
	}
	public void setSellBrokerage(String sellBrokerage) {
		this.sellBrokerage = sellBrokerage;
	}
	public String getSellWapAfterBrokerage() {
		return sellWapAfterBrokerage;
	}
	public void setSellWapAfterBrokerage(String sellWapAfterBrokerage) {
		this.sellWapAfterBrokerage = sellWapAfterBrokerage;
	}
	public String getSellValue() {
		return sellValue;
	}
	public void setSellValue(String sellValue) {
		this.sellValue = sellValue;
	}
	public String getNetQty() {
		return netQty;
	}
	public void setNetQty(String netQty) {
		this.netQty = netQty;
	}
	public String getNetObligationIsin() {
		return netObligationIsin;
	}
	public void setNetObligationIsin(String netObligationIsin) {
		this.netObligationIsin = netObligationIsin;
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
	public String getTradeQty() {
		return tradeQty;
	}
	public void setTradeQty(String tradeQty) {
		this.tradeQty = tradeQty;
	}
	public String getTradeWapFc() {
		return tradeWapFc;
	}
	public void setTradeWapFc(String tradeWapFc) {
		this.tradeWapFc = tradeWapFc;
	}
	public String getTradeWap() {
		return tradeWap;
	}
	public void setTradeWap(String tradeWap) {
		this.tradeWap = tradeWap;
	}
	public String getTradeBrokerage() {
		return tradeBrokerage;
	}
	public void setTradeBrokerage(String tradeBrokerage) {
		this.tradeBrokerage = tradeBrokerage;
	}
	public String getTradeWapAfterBrokerage() {
		return tradeWapAfterBrokerage;
	}
	public void setTradeWapAfterBrokerage(String tradeWapAfterBrokerage) {
		this.tradeWapAfterBrokerage = tradeWapAfterBrokerage;
	}
	public String getTradeClosingRate() {
		return tradeClosingRate;
	}
	public void setTradeClosingRate(String tradeClosingRate) {
		this.tradeClosingRate = tradeClosingRate;
	}
	public String getTradeNetTotal() {
		return tradeNetTotal;
	}
	public void setTradeNetTotal(String tradeNetTotal) {
		this.tradeNetTotal = tradeNetTotal;
	}
	public String getRemarks() {
		return remarks;
	}
	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}
	
	@Override
	public String toString() {
		return "CNSecuritySummaryDTO [tradeCode=" + tradeCode + ", recordType=" + recordType + ", segment=" + segment
				+ ", isin=" + isin + ", securityDescription=" + securityDescription + ", buyQty=" + buyQty + ", buyWap="
				+ buyWap + ", buyBrokerage=" + buyBrokerage + ", buyWapAfterBrokerage=" + buyWapAfterBrokerage
				+ ", buyValue=" + buyValue + ", sellQty=" + sellQty + ", sellWap=" + sellWap + ", sellBrokerage="
				+ sellBrokerage + ", sellWapAfterBrokerage=" + sellWapAfterBrokerage + ", sellValue=" + sellValue
				+ ", netQty=" + netQty + ", netObligationIsin=" + netObligationIsin + ", contractDesc=" + contractDesc
				+ ", tradeType=" + tradeType + ", tradeQty=" + tradeQty + ", tradeWapFc=" + tradeWapFc + ", tradeWap="
				+ tradeWap + ", tradeBrokerage=" + tradeBrokerage + ", tradeWapAfterBrokerage="
				+ tradeWapAfterBrokerage + ", tradeClosingRate=" + tradeClosingRate + ", tradeNetTotal="
				+ tradeNetTotal + ", remarks=" + remarks + "]";
	}
}