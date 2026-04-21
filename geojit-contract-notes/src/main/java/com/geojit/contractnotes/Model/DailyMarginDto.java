package com.geojit.contractnotes.Model;


public class DailyMarginDto {

//    <Unique Id>~ G~ <Seq>~ <Trade Day>~ <Funds>~ <Value of Securities>~
//    <Value of Margin Pledge Securities>~ <Bank Guarantees/FDR>~
//    <Any Other Approved form of Margins>~ <Total Margins Available(E)>~
//    <Total upfront Margin>~ <Consolidated Crystallised>~ <Delivery Margin>~
//    <Total Requirement>~ <Excess/Shortfall w.r.t>~
//    <Additional Margins Required by member as per RMS>~ <Margin Status(Balance with member)>
    private String uniqueId;                                // Field 0
    private String recordType;                              // Field 1 - Always "G"
    private String seq;                                     // Field 2
    private String tradeDay;                                // Field 3
    private String funds;                                   // Field 4
    private String valueOfSecurities;                       // Field 5
    private String valueOfMarginPledgeSecurities;           // Field 6
    private String bankGuaranteesFdr;                       // Field 7
    private String anyOtherApprovedFormOfMargins;           // Field 8
    private String totalMarginsAvailable;                   // Field 9
    private String totalUpfrontMargin;                      // Field 10
    private String consolidatedCrystallised;                // Field 11
    private String deliveryMargin;                          // Field 12
    private String totalRequirement;                        // Field 13
    private String excessShortfall;                         // Field 14
    private String additionalMarginsRequiredByMemberAsPerRMS; // Field 15
    private String marginStatusBalanceWithMember;           // Field 16

    public DailyMarginDto() {
    }

    public DailyMarginDto(String uniqueId, String recordType, String seq, String tradeDay,
                          String funds, String valueOfSecurities, String valueOfMarginPledgeSecurities,
                          String bankGuaranteesFdr, String anyOtherApprovedFormOfMargins,
                          String totalMarginsAvailable, String totalUpfrontMargin,
                          String consolidatedCrystallised, String deliveryMargin,
                          String totalRequirement, String excessShortfall,
                          String additionalMarginsRequiredByMemberAsPerRMS,
                          String marginStatusBalanceWithMember) {
        this.uniqueId = uniqueId;
        this.recordType = recordType;
        this.seq = seq;
        this.tradeDay = tradeDay;
        this.funds = funds;
        this.valueOfSecurities = valueOfSecurities;
        this.valueOfMarginPledgeSecurities = valueOfMarginPledgeSecurities;
        this.bankGuaranteesFdr = bankGuaranteesFdr;
        this.anyOtherApprovedFormOfMargins = anyOtherApprovedFormOfMargins;
        this.totalMarginsAvailable = totalMarginsAvailable;
        this.totalUpfrontMargin = totalUpfrontMargin;
        this.consolidatedCrystallised = consolidatedCrystallised;
        this.deliveryMargin = deliveryMargin;
        this.totalRequirement = totalRequirement;
        this.excessShortfall = excessShortfall;
        this.additionalMarginsRequiredByMemberAsPerRMS = additionalMarginsRequiredByMemberAsPerRMS;
        this.marginStatusBalanceWithMember = marginStatusBalanceWithMember;
    }

    // Getters and Setters
    public String getUniqueId() { return uniqueId; }
    public void setUniqueId(String uniqueId) { this.uniqueId = uniqueId; }

    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }

    public String getSeq() { return seq; }
    public void setSeq(String seq) { this.seq = seq; }

    public String getTradeDay() { return tradeDay; }
    public void setTradeDay(String tradeDay) { this.tradeDay = tradeDay; }

    public String getFunds() { return funds; }
    public void setFunds(String funds) { this.funds = funds; }

    public String getValueOfSecurities() { return valueOfSecurities; }
    public void setValueOfSecurities(String valueOfSecurities) { this.valueOfSecurities = valueOfSecurities; }

    public String getValueOfMarginPledgeSecurities() { return valueOfMarginPledgeSecurities; }
    public void setValueOfMarginPledgeSecurities(String valueOfMarginPledgeSecurities) { this.valueOfMarginPledgeSecurities = valueOfMarginPledgeSecurities; }

    public String getBankGuaranteesFdr() { return bankGuaranteesFdr; }
    public void setBankGuaranteesFdr(String bankGuaranteesFdr) { this.bankGuaranteesFdr = bankGuaranteesFdr; }

    public String getAnyOtherApprovedFormOfMargins() { return anyOtherApprovedFormOfMargins; }
    public void setAnyOtherApprovedFormOfMargins(String anyOtherApprovedFormOfMargins) { this.anyOtherApprovedFormOfMargins = anyOtherApprovedFormOfMargins; }

    public String getTotalMarginsAvailable() { return totalMarginsAvailable; }
    public void setTotalMarginsAvailable(String totalMarginsAvailable) { this.totalMarginsAvailable = totalMarginsAvailable; }

    public String getTotalUpfrontMargin() { return totalUpfrontMargin; }
    public void setTotalUpfrontMargin(String totalUpfrontMargin) { this.totalUpfrontMargin = totalUpfrontMargin; }

    public String getConsolidatedCrystallised() { return consolidatedCrystallised; }
    public void setConsolidatedCrystallised(String consolidatedCrystallised) { this.consolidatedCrystallised = consolidatedCrystallised; }

    public String getDeliveryMargin() { return deliveryMargin; }
    public void setDeliveryMargin(String deliveryMargin) { this.deliveryMargin = deliveryMargin; }

    public String getTotalRequirement() { return totalRequirement; }
    public void setTotalRequirement(String totalRequirement) { this.totalRequirement = totalRequirement; }

    public String getExcessShortfall() { return excessShortfall; }
    public void setExcessShortfall(String excessShortfall) { this.excessShortfall = excessShortfall; }

    public String getAdditionalMarginsRequiredByMemberAsPerRMS() { return additionalMarginsRequiredByMemberAsPerRMS; }
    public void setAdditionalMarginsRequiredByMemberAsPerRMS(String additionalMarginsRequiredByMemberAsPerRMS) { this.additionalMarginsRequiredByMemberAsPerRMS = additionalMarginsRequiredByMemberAsPerRMS; }

    public String getMarginStatusBalanceWithMember() { return marginStatusBalanceWithMember; }
    public void setMarginStatusBalanceWithMember(String marginStatusBalanceWithMember) { this.marginStatusBalanceWithMember = marginStatusBalanceWithMember; }

    @Override
    public String toString() {
        return "GeneralDto{" +
                "uniqueId='" + uniqueId + '\'' +
                ", recordType='" + recordType + '\'' +
                ", seq='" + seq + '\'' +
                ", tradeDay='" + tradeDay + '\'' +
                ", funds='" + funds + '\'' +
                '}';
    }
}