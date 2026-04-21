package com.geojit.contractnotes.Model;

/**
 * DTO for Geojit Total (T) tag
 * Format: <Unique Id>|T|<Total>|<Securities Transactions Tax>|<Net Amount>
 * Expected field count: 5
 */
public class NetObligationTotalDto {
    private String uniqueId;                    // Field 0
    private String recordType;                  // Field 1 - Always "T"
    private String total;                       // Field 2
    private String securitiesTransactionsTax;   // Field 3
    private String netAmount;                   // Field 4

    public NetObligationTotalDto() {
    }

    public NetObligationTotalDto(String uniqueId, String recordType, String total,
                                 String securitiesTransactionsTax, String netAmount) {
        this.uniqueId = uniqueId;
        this.recordType = recordType;
        this.total = total;
        this.securitiesTransactionsTax = securitiesTransactionsTax;
        this.netAmount = netAmount;
    }

    public String getUniqueId() { return uniqueId; }
    public void setUniqueId(String uniqueId) { this.uniqueId = uniqueId; }

    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }

    public String getTotal() { return total; }
    public void setTotal(String total) { this.total = total; }

    public String getSecuritiesTransactionsTax() { return securitiesTransactionsTax; }
    public void setSecuritiesTransactionsTax(String securitiesTransactionsTax) { this.securitiesTransactionsTax = securitiesTransactionsTax; }

    public String getNetAmount() { return netAmount; }
    public void setNetAmount(String netAmount) { this.netAmount = netAmount; }

    @Override
    public String toString() {
        return "TotalDto{" +
                "uniqueId='" + uniqueId + '\'' +
                ", recordType='" + recordType + '\'' +
                ", total='" + total + '\'' +
                ", securitiesTransactionsTax='" + securitiesTransactionsTax + '\'' +
                ", netAmount='" + netAmount + '\'' +
                '}';
    }
}