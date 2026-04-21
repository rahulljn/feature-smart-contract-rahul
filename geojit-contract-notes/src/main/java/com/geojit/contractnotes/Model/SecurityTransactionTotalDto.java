package com.geojit.contractnotes.Model;

public class SecurityTransactionTotalDto {
    private String uniqueId;                // Field 0
    private String recordType;              // Field 1 - Always "R"
    private String totalRoundedToNearestRupee; // Field 2

    public SecurityTransactionTotalDto() {
    }

    public SecurityTransactionTotalDto(String uniqueId, String recordType, String totalRoundedToNearestRupee) {
        this.uniqueId = uniqueId;
        this.recordType = recordType;
        this.totalRoundedToNearestRupee = totalRoundedToNearestRupee;
    }

    public String getUniqueId() { return uniqueId; }
    public void setUniqueId(String uniqueId) { this.uniqueId = uniqueId; }

    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }

    public String getTotalRoundedToNearestRupee() { return totalRoundedToNearestRupee; }
    public void setTotalRoundedToNearestRupee(String totalRoundedToNearestRupee) { this.totalRoundedToNearestRupee = totalRoundedToNearestRupee; }

    @Override
    public String toString() {
        return "RoundedTotalDto{" +
                "uniqueId='" + uniqueId + '\'' +
                ", recordType='" + recordType + '\'' +
                ", totalRoundedToNearestRupee='" + totalRoundedToNearestRupee + '\'' +
                '}';
    }
}