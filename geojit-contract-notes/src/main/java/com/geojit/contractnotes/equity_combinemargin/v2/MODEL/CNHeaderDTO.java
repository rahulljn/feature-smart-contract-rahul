package com.geojit.contractnotes.equity_combinemargin.v2.MODEL;

public class CNHeaderDTO {
	
	String tradeCode;
	String recordType; // H
	String uccCode;
	String clientName;
	String clientAddress1;
	String clientAddress2;
	String clientAddress3;
	String contractNoteNo;
	String clientPan;
	String tradeDate;
	String clientEmail;
	String clientMobile;
	String gstNo;
	String irn;
	String businessType;
	
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
	public String getUccCode() {
		return uccCode;
	}
	public void setUccCode(String uccCode) {
		this.uccCode = uccCode;
	}
	public String getClientName() {
		return clientName;
	}
	public void setClientName(String clientName) {
		this.clientName = clientName;
	}
	public String getClientAddress1() {
		return clientAddress1;
	}
	public void setClientAddress1(String clientAddress1) {
		this.clientAddress1 = clientAddress1;
	}
	public String getClientAddress2() {
		return clientAddress2;
	}
	public void setClientAddress2(String clientAddress2) {
		this.clientAddress2 = clientAddress2;
	}
	public String getClientAddress3() {
		return clientAddress3;
	}
	public void setClientAddress3(String clientAddress3) {
		this.clientAddress3 = clientAddress3;
	}
	public String getContractNoteNo() {
		return contractNoteNo;
	}
	public void setContractNoteNo(String contractNoteNo) {
		this.contractNoteNo = contractNoteNo;
	}
	public String getClientPan() {
		return clientPan;
	}
	public void setClientPan(String clientPan) {
		this.clientPan = clientPan;
	}
	public String getTradeDate() {
		return tradeDate;
	}
	public void setTradeDate(String tradeDate) {
		this.tradeDate = tradeDate;
	}
	public String getClientEmail() {
		return clientEmail;
	}
	public void setClientEmail(String clientEmail) {
		this.clientEmail = clientEmail;
	}
	public String getClientMobile() {
		return clientMobile;
	}
	public void setClientMobile(String clientMobile) {
		this.clientMobile = clientMobile;
	}
	public String getGstNo() {
		return gstNo;
	}
	public void setGstNo(String gstNo) {
		this.gstNo = gstNo;
	}
	public String getIrn() {
		return irn;
	}
	public void setIrn(String irn) {
		this.irn = irn;
	}
	public String getBusinessType() {
		return businessType;
	}
	public void setBusinessType(String businessType) {
		this.businessType = businessType;
	}
	
	@Override
	public String toString() {
		return "CNHeaderDTO [tradeCode=" + tradeCode + ", recordType=" + recordType + ", uccCode=" + uccCode
				+ ", clientName=" + clientName + ", clientAddress1=" + clientAddress1 + ", clientAddress2="
				+ clientAddress2 + ", clientAddress3=" + clientAddress3 + ", contractNoteNo=" + contractNoteNo
				+ ", clientPan=" + clientPan + ", tradeDate=" + tradeDate + ", clientEmail=" + clientEmail
				+ ", clientMobile=" + clientMobile + ", gstNo=" + gstNo + ", irn=" + irn + ", businessType="
				+ businessType + "]";
	}
}