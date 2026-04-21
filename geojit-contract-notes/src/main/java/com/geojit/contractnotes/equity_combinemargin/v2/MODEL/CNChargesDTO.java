package com.geojit.contractnotes.equity_combinemargin.v2.MODEL;

public class CNChargesDTO {
	
	String tradeCode;
	String recordType; // C
	String chargeType;
	String tradeDate;
	
	// For SEBI type
	String chargeAmount;
	String fundBalance;
	
	// For MTM type
	String nsefo;
	String nsecds;
	String bsefo;
	String mcxcds;
	String bsecds;
	String mcx;
	String ncdex;
	String icex;
	
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
	public String getChargeType() {
		return chargeType;
	}
	public void setChargeType(String chargeType) {
		this.chargeType = chargeType;
	}
	public String getTradeDate() {
		return tradeDate;
	}
	public void setTradeDate(String tradeDate) {
		this.tradeDate = tradeDate;
	}
	public String getChargeAmount() {
		return chargeAmount;
	}
	public void setChargeAmount(String chargeAmount) {
		this.chargeAmount = chargeAmount;
	}
	public String getFundBalance() {
		return fundBalance;
	}
	public void setFundBalance(String fundBalance) {
		this.fundBalance = fundBalance;
	}
	public String getNsefo() {
		return nsefo;
	}
	public void setNsefo(String nsefo) {
		this.nsefo = nsefo;
	}
	public String getNsecds() {
		return nsecds;
	}
	public void setNsecds(String nsecds) {
		this.nsecds = nsecds;
	}
	public String getBsefo() {
		return bsefo;
	}
	public void setBsefo(String bsefo) {
		this.bsefo = bsefo;
	}
	public String getMcxcds() {
		return mcxcds;
	}
	public void setMcxcds(String mcxcds) {
		this.mcxcds = mcxcds;
	}
	public String getBsecds() {
		return bsecds;
	}
	public void setBsecds(String bsecds) {
		this.bsecds = bsecds;
	}
	public String getMcx() {
		return mcx;
	}
	public void setMcx(String mcx) {
		this.mcx = mcx;
	}
	public String getNcdex() {
		return ncdex;
	}
	public void setNcdex(String ncdex) {
		this.ncdex = ncdex;
	}
	public String getIcex() {
		return icex;
	}
	public void setIcex(String icex) {
		this.icex = icex;
	}
	
	@Override
	public String toString() {
		return "CNChargesDTO [tradeCode=" + tradeCode + ", recordType=" + recordType + ", chargeType=" + chargeType
				+ ", tradeDate=" + tradeDate + ", chargeAmount=" + chargeAmount + ", fundBalance=" + fundBalance
				+ ", nsefo=" + nsefo + ", nsecds=" + nsecds + ", bsefo=" + bsefo + ", mcxcds=" + mcxcds + ", bsecds="
				+ bsecds + ", mcx=" + mcx + ", ncdex=" + ncdex + ", icex=" + icex + "]";
	}
}