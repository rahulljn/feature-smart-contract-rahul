package com.geojit.contractnotes.equity_combinemargin.v2.MODEL;

public class CNAddressDTO {
	
	String tradeCode;
	String recordType; // A
	String dealingOfficeAddress;
	String gstLocation;
	String dealingOfficeNo;
	String gstin;
	
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
	public String getDealingOfficeAddress() {
		return dealingOfficeAddress;
	}
	public void setDealingOfficeAddress(String dealingOfficeAddress) {
		this.dealingOfficeAddress = dealingOfficeAddress;
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
	public String getGstin() {
		return gstin;
	}
	public void setGstin(String gstin) {
		this.gstin = gstin;
	}
	
	@Override
	public String toString() {
		return "CNAddressDTO [tradeCode=" + tradeCode + ", recordType=" + recordType + ", dealingOfficeAddress="
				+ dealingOfficeAddress + ", gstLocation=" + gstLocation + ", dealingOfficeNo=" + dealingOfficeNo
				+ ", gstin=" + gstin + "]";
	}
}