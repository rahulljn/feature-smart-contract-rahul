package com.geojit.contractnotes.Model;


public class HeaderDto {

    //<Unique Id>~ H~ <Contract Note No>~
    // <Trade Date>~ <Name of Client>~ <Address>~
    // <Phone no>~ <Trade Code>~ <Place of Supply>~
    // <Invoice Reference Number>~ <GST Identification No>~ <PAN Of Client>
    private String uniqueId;                   // Field 0
    private String recordType;                 // Field 1 - Always "H"
    private String contractNoteNo;             // Field 2
    private String tradeDate;                  // Field 3
    private String nameOfClient;               // Field 4
    private String address;                    // Field 5
    private String phoneNo;                    // Field 6
    private String tradeCode;                  // Field 7
    private String placeOfSupply;              // Field 8
    private String invoiceReferenceNumber;     // Field 9
    private String gstIdentificationNo;        // Field 10
    private String panOfClient;                // Field 11
    private String email;
    private String dealingOfficeAddress;
    private String telephoneNo;

    public String getDealingOfficeAddress() {
        return dealingOfficeAddress;
    }

    public void setDealingOfficeAddress(String dealingOfficeAddress) {
        this.dealingOfficeAddress = dealingOfficeAddress;
    }

    public String getTelephoneNo() {
        return telephoneNo;
    }

    public void setTelephoneNo(String telephoneNo) {
        this.telephoneNo = telephoneNo;
    }



    public HeaderDto() {}

    public HeaderDto(String uniqueId, String recordType, String contractNoteNo, String tradeDate,
                     String nameOfClient, String address, String phoneNo, String tradeCode,
                     String placeOfSupply, String invoiceReferenceNumber, String gstIdentificationNo,
                     String panOfClient,String email, String dealingOfficeAddress, String telephoneNo) {
        this.uniqueId = uniqueId;
        this.recordType = recordType;
        this.contractNoteNo = contractNoteNo;
        this.tradeDate = tradeDate;
        this.nameOfClient = nameOfClient;
        this.address = address;
        this.phoneNo = phoneNo;
        this.tradeCode = tradeCode;
        this.placeOfSupply = placeOfSupply;
        this.invoiceReferenceNumber = invoiceReferenceNumber;
        this.gstIdentificationNo = gstIdentificationNo;
        this.panOfClient = panOfClient;
        this.email = email;
        this.dealingOfficeAddress = dealingOfficeAddress;
        this.telephoneNo = telephoneNo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUniqueId() { return uniqueId; }
    public void setUniqueId(String uniqueId) { this.uniqueId = uniqueId; }

    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }

    public String getContractNoteNo() { return contractNoteNo; }
    public void setContractNoteNo(String contractNoteNo) { this.contractNoteNo = contractNoteNo; }

    public String getTradeDate() { return tradeDate; }
    public void setTradeDate(String tradeDate) { this.tradeDate = tradeDate; }

    public String getNameOfClient() { return nameOfClient; }
    public void setNameOfClient(String nameOfClient) { this.nameOfClient = nameOfClient; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhoneNo() { return phoneNo; }
    public void setPhoneNo(String phoneNo) { this.phoneNo = phoneNo; }

    public String getTradeCode() { return tradeCode; }
    public void setTradeCode(String tradeCode) { this.tradeCode = tradeCode; }

    public String getPlaceOfSupply() { return placeOfSupply; }
    public void setPlaceOfSupply(String placeOfSupply) { this.placeOfSupply = placeOfSupply; }

    public String getInvoiceReferenceNumber() { return invoiceReferenceNumber; }
    public void setInvoiceReferenceNumber(String invoiceReferenceNumber) { this.invoiceReferenceNumber = invoiceReferenceNumber; }

    public String getGstIdentificationNo() { return gstIdentificationNo; }
    public void setGstIdentificationNo(String gstIdentificationNo) { this.gstIdentificationNo = gstIdentificationNo; }

    public String getPanOfClient() { return panOfClient; }
    public void setPanOfClient(String panOfClient) { this.panOfClient = panOfClient; }


    @Override
    public String toString() {
        return "HeaderDto{" +
                "uniqueId='" + uniqueId + '\'' +
                ", recordType='" + recordType + '\'' +
                ", contractNoteNo='" + contractNoteNo + '\'' +
                ", tradeDate='" + tradeDate + '\'' +
                ", nameOfClient='" + nameOfClient + '\'' +
                ", panOfClient='" + panOfClient + '\'' +
                '}';
    }
}