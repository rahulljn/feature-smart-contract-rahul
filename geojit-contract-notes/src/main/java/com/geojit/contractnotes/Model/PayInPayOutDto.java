package com.geojit.contractnotes.Model;


public class PayInPayOutDto {

//    <Unique Id>~ O~ <Name of Exchange & Segment>~ <Pay in/Pay Out>~
//    <Securities Transactions>~ <SGST>~ <CGST>~ <IGST>~ <TDS>~
//    <Exchange Transaction Charges>~ <SEBI turnover>~ <Additional Cess>~
//    <Stampt Duty>~ <Net Amount Receivable By Client>
    private String uniqueId;                    // Field 0
    private String recordType;                  // Field 1 - Always "O"
    private String nameOfExchangeSegment;       // Field 2
    private String payInPayOut;                 // Field 3
    private String securitiesTransactions;      // Field 4
    private String sgst;                        // Field 5
    private String cgst;                        // Field 6
    private String igst;                        // Field 7
    private String tds;                         // Field 8
    private String exchangeTransactionCharges;  // Field 9
    private String sebiTurnover;                // Field 10
    private String additionalCess;              // Field 11
    private String stampDuty;                   // Field 12
    private String netAmountReceivableByClient; // Field 13

    public PayInPayOutDto() {
    }

    public PayInPayOutDto(String uniqueId, String recordType, String nameOfExchangeSegment,
                          String payInPayOut, String securitiesTransactions, String sgst,
                          String cgst, String igst, String tds, String exchangeTransactionCharges,
                          String sebiTurnover, String additionalCess, String stampDuty,
                          String netAmountReceivableByClient) {
        this.uniqueId = uniqueId;
        this.recordType = recordType;
        this.nameOfExchangeSegment = nameOfExchangeSegment;
        this.payInPayOut = payInPayOut;
        this.securitiesTransactions = securitiesTransactions;
        this.sgst = sgst;
        this.cgst = cgst;
        this.igst = igst;
        this.tds = tds;
        this.exchangeTransactionCharges = exchangeTransactionCharges;
        this.sebiTurnover = sebiTurnover;
        this.additionalCess = additionalCess;
        this.stampDuty = stampDuty;
        this.netAmountReceivableByClient = netAmountReceivableByClient;
    }

    // Getters and Setters
    public String getUniqueId() { return uniqueId; }
    public void setUniqueId(String uniqueId) { this.uniqueId = uniqueId; }

    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }

    public String getNameOfExchangeSegment() { return nameOfExchangeSegment; }
    public void setNameOfExchangeSegment(String nameOfExchangeSegment) { this.nameOfExchangeSegment = nameOfExchangeSegment; }

    public String getPayInPayOut() { return payInPayOut; }
    public void setPayInPayOut(String payInPayOut) { this.payInPayOut = payInPayOut; }

    public String getSecuritiesTransactions() { return securitiesTransactions; }
    public void setSecuritiesTransactions(String securitiesTransactions) { this.securitiesTransactions = securitiesTransactions; }

    public String getSgst() { return sgst; }
    public void setSgst(String sgst) { this.sgst = sgst; }

    public String getCgst() { return cgst; }
    public void setCgst(String cgst) { this.cgst = cgst; }

    public String getIgst() { return igst; }
    public void setIgst(String igst) { this.igst = igst; }

    public String getTds() { return tds; }
    public void setTds(String tds) { this.tds = tds; }

    public String getExchangeTransactionCharges() { return exchangeTransactionCharges; }
    public void setExchangeTransactionCharges(String exchangeTransactionCharges) { this.exchangeTransactionCharges = exchangeTransactionCharges; }

    public String getSebiTurnover() { return sebiTurnover; }
    public void setSebiTurnover(String sebiTurnover) { this.sebiTurnover = sebiTurnover; }

    public String getAdditionalCess() { return additionalCess; }
    public void setAdditionalCess(String additionalCess) { this.additionalCess = additionalCess; }

    public String getStampDuty() { return stampDuty; }
    public void setStampDuty(String stampDuty) { this.stampDuty = stampDuty; }

    public String getNetAmountReceivableByClient() { return netAmountReceivableByClient; }
    public void setNetAmountReceivableByClient(String netAmountReceivableByClient) { this.netAmountReceivableByClient = netAmountReceivableByClient; }

    @Override
    public String toString() {
        return "ObligationDto{" +
                "uniqueId='" + uniqueId + '\'' +
                ", recordType='" + recordType + '\'' +
                ", nameOfExchangeSegment='" + nameOfExchangeSegment + '\'' +
                ", payInPayOut='" + payInPayOut + '\'' +
                ", securitiesTransactions='" + securitiesTransactions + '\'' +
                '}';
    }
}