package com.geojit.contractnotes.equity_combinemargin.v1.Model;

public class CustomerModel {

    private String partycode;           // TRADE_CODE
    private String headerType;          // H
    private String clientCode;          // UCC_CODE
    private String name;                // CLIENT_NAME
    private String address1;            // CLIENT_ADDRESS_1
    private String address2;            // CLIENT_ADDRESS_2
    private String address3;            // CLIENT_ADDRESS_3
    private String contractNo;          // CONTRACT_NOTE_NO
    private String panNo;               // CLIENT_PAN
    private String transactionDate;     // TRADE_DATE
    private String email;               // CLIENT_EMAIL
    private String mobileNo;            // CLIENT_MOBILE
    private String gstNo;               // GST_NO
    private String irn;                 // IRN (was ledgerBalance)
    private String buinessType;         // BUSINESS_TYPE
    private String ecnFlag;             // Optional field for ECN flag

    public String getPartycode() {
        return partycode;
    }

    public void setPartycode(String partycode) {
        this.partycode = partycode;
    }

    public String getHeaderType() {
        return headerType;
    }

    public void setHeaderType(String headerType) {
        this.headerType = headerType;
    }

    public String getClientCode() {
        return clientCode;
    }

    public void setClientCode(String clientCode) {
        this.clientCode = clientCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress1() {
        return address1;
    }

    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    public String getAddress2() {
        return address2;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    public String getAddress3() {
        return address3;
    }

    public void setAddress3(String address3) {
        this.address3 = address3;
    }

    public String getContractNo() {
        return contractNo;
    }

    public void setContractNo(String contractNo) {
        this.contractNo = contractNo;
    }

    public String getPanNo() {
        return panNo;
    }

    public void setPanNo(String panNo) {
        this.panNo = panNo;
    }

    public String getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(String transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobileNo() {
        return mobileNo;
    }

    public void setMobileNo(String mobileNo) {
        this.mobileNo = mobileNo;
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

    public String getBuinessType() {
        return buinessType;
    }

    public void setBuinessType(String buinessType) {
        this.buinessType = buinessType;
    }

    public String getEcnFlag() {
        return ecnFlag;
    }

    public void setEcnFlag(String ecnFlag) {
        this.ecnFlag = ecnFlag;
    }

    @Override
    public String toString() {
        return "CustomerModel [partycode=" + partycode + ", headerType=" + headerType +
                ", clientCode=" + clientCode + ", name=" + name +
                ", address1=" + address1 + ", address2=" + address2 +
                ", address3=" + address3 + ", contractNo=" + contractNo +
                ", panNo=" + panNo + ", transactionDate=" + transactionDate +
                ", email=" + email + ", mobileNo=" + mobileNo +
                ", gstNo=" + gstNo + ", irn=" + irn +
                ", buinessType=" + buinessType + ", ecnFlag=" + ecnFlag + "]";
    }
}