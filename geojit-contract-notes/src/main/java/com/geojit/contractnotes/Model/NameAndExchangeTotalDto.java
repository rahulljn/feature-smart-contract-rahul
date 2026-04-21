package com.geojit.contractnotes.Model;

import java.util.ArrayList;
import java.util.List;

// ═══════════════════════════════════════════════════════════════════════
// CHANGE 1 — NameAndExchangeTotalDto.java
// ADD: import java.util.ArrayList + java.util.List  (at top)
// ADD: private List<NameClearingCorporationDto> details field + getter/setter
// KEEP: everything else exactly the same
// ═══════════════════════════════════════════════════════════════════════
public class NameAndExchangeTotalDto {

    //    <Unique Id>~ N~ <Name Of Exchange>~ <Segment>~
//    <Security/Contract Description>~ <Buy/Sell>~ <Quantity>~
//    <Gross Rate(in foreign currency)>~ <Gross Rate(Rs)>~ <Brokerage>~
//    <Net Rate>~ <Closing Rate>~ <Net Total>~ <Remarks>
    private String uniqueId;                    // Field 0
    private String recordType;                  // Field 1 - Always "N"
    private String nameOfExchange;              // Field 2
    private String segment;                     // Field 3
    private String securityContractDescription; // Field 4
    private String buySell;                     // Field 5
    private String quantity;                    // Field 6
    private String grossRateForeignCurrency;    // Field 7
    private String grossRateRs;                 // Field 8
    private String brokerage;                   // Field 9
    private String netRate;                     // Field 10
    private String closingRate;                 // Field 11
    private String netTotal;                    // Field 12
    private String remarks;                     // Field 13

    // ✅ NEW FIELD — embedded D-records for this N section
    // Each N record owns its own D records sequentially
    private List<NameClearingCorporationDto> details = new ArrayList<>();

    public NameAndExchangeTotalDto() {
    }

    public NameAndExchangeTotalDto(String uniqueId, String recordType, String nameOfExchange, String segment,
                                   String securityContractDescription, String buySell, String quantity,
                                   String grossRateForeignCurrency, String grossRateRs, String brokerage,
                                   String netRate, String closingRate, String netTotal, String remarks) {
        this.uniqueId = uniqueId;
        this.recordType = recordType;
        this.nameOfExchange = nameOfExchange;
        this.segment = segment;
        this.securityContractDescription = securityContractDescription;
        this.buySell = buySell;
        this.quantity = quantity;
        this.grossRateForeignCurrency = grossRateForeignCurrency;
        this.grossRateRs = grossRateRs;
        this.brokerage = brokerage;
        this.netRate = netRate;
        this.closingRate = closingRate;
        this.netTotal = netTotal;
        this.remarks = remarks;
    }

    // ✅ NEW getter/setter for embedded D-records
    public List<NameClearingCorporationDto> getDetails() { return details; }
    public void setDetails(List<NameClearingCorporationDto> details) { this.details = details; }

    // keep all existing getters/setters exactly as before
    public String getUniqueId() { return uniqueId; }
    public void setUniqueId(String uniqueId) { this.uniqueId = uniqueId; }

    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }

    public String getNameOfExchange() { return nameOfExchange; }
    public void setNameOfExchange(String nameOfExchange) { this.nameOfExchange = nameOfExchange; }

    public String getSegment() { return segment; }
    public void setSegment(String segment) { this.segment = segment; }

    public String getSecurityContractDescription() { return securityContractDescription; }
    public void setSecurityContractDescription(String securityContractDescription) { this.securityContractDescription = securityContractDescription; }

    public String getBuySell() { return buySell; }
    public void setBuySell(String buySell) { this.buySell = buySell; }

    public String getQuantity() { return quantity; }
    public void setQuantity(String quantity) { this.quantity = quantity; }

    public String getGrossRateForeignCurrency() { return grossRateForeignCurrency; }
    public void setGrossRateForeignCurrency(String grossRateForeignCurrency) { this.grossRateForeignCurrency = grossRateForeignCurrency; }

    public String getGrossRateRs() { return grossRateRs; }
    public void setGrossRateRs(String grossRateRs) { this.grossRateRs = grossRateRs; }

    public String getBrokerage() { return brokerage; }
    public void setBrokerage(String brokerage) { this.brokerage = brokerage; }

    public String getNetRate() { return netRate; }
    public void setNetRate(String netRate) { this.netRate = netRate; }

    public String getClosingRate() { return closingRate; }
    public void setClosingRate(String closingRate) { this.closingRate = closingRate; }

    public String getNetTotal() { return netTotal; }
    public void setNetTotal(String netTotal) { this.netTotal = netTotal; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    @Override
    public String toString() {
        return "NoteDto{" +
                "uniqueId='" + uniqueId + '\'' +
                ", recordType='" + recordType + '\'' +
                ", nameOfExchange='" + nameOfExchange + '\'' +
                ", segment='" + segment + '\'' +
                ", securityContractDescription='" + securityContractDescription + '\'' +
                '}';
    }
}