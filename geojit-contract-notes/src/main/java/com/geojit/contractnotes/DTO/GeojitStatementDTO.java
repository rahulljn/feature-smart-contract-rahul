package com.geojit.contractnotes.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.geojit.contractnotes.Model.*;

import java.util.List;

public class GeojitStatementDTO {

    private HeaderDto header;
    private List<ExchangeClearingDto>   exchanges;
    private List<EquitySegmentDto>   positions;
    @JsonProperty("vRecords")
    private List<DerivativeSegmentDto>          vRecords;
    private List<NameClearingCorporationDto>     details;
    private List<PayInPayOutDto> obligations;
    private DatePlaceDto footer;
    private List<NameAndExchangeTotalDto>       notes;
    private List<NetObligationDto>     margins;
    private NetObligationTotalDto total;
    @JsonProperty("uRecords")
    private List<ScripSummaryDto>          uRecords;         // Tag U - Position Summary (11 fields)
    private List<SecurityTransactionDto>   contracts;
    private SecurityTransactionTotalDto roundedTotal;
    private List<CashSegmentTotalDto>     amounts;
    private List<CashSegmentDto>        lots;
    private List<DailyMarginDto>    generals;
    private DailyMarginTotalDto journal;
    private List<MarginPledgeDto>   securities;
    private MarginPledgeTotalDto quantity;

    public GeojitStatementDTO() {}

    public HeaderDto getHeader() { return header; }
    public void setHeader(HeaderDto header) { this.header = header; }

    public List<ExchangeClearingDto> getExchanges() { return exchanges; }
    public void setExchanges(List<ExchangeClearingDto> exchanges) { this.exchanges = exchanges; }

    public List<EquitySegmentDto> getPositions() { return positions; }
    public void setPositions(List<EquitySegmentDto> positions) { this.positions = positions; }

    public List<DerivativeSegmentDto> getVRecords() { return vRecords; }
    public void setVRecords(List<DerivativeSegmentDto> vRecords) { this.vRecords = vRecords; }

    public List<NameClearingCorporationDto> getDetails() { return details; }
    public void setDetails(List<NameClearingCorporationDto> details) { this.details = details; }

    public List<PayInPayOutDto> getObligations() { return obligations; }
    public void setObligations(List<PayInPayOutDto> obligations) { this.obligations = obligations; }

    public DatePlaceDto getFooter() { return footer; }
    public void setFooter(DatePlaceDto footer) { this.footer = footer; }

    public List<NameAndExchangeTotalDto> getNotes() { return notes; }
    public void setNotes(List<NameAndExchangeTotalDto> notes) { this.notes = notes; }

    public List<NetObligationDto> getMargins() { return margins; }
    public void setMargins(List<NetObligationDto> margins) { this.margins = margins; }

    public NetObligationTotalDto getTotal() { return total; }
    public void setTotal(NetObligationTotalDto total) { this.total = total; }

    public List<ScripSummaryDto> getURecords() { return uRecords; }
    public void setURecords(List<ScripSummaryDto> uRecords) { this.uRecords = uRecords; }

    public List<SecurityTransactionDto> getContracts() { return contracts; }
    public void setContracts(List<SecurityTransactionDto> contracts) { this.contracts = contracts; }

    public SecurityTransactionTotalDto getRoundedTotal() { return roundedTotal; }
    public void setRoundedTotal(SecurityTransactionTotalDto roundedTotal) { this.roundedTotal = roundedTotal; }

    public List<CashSegmentTotalDto> getAmounts() { return amounts; }
    public void setAmounts(List<CashSegmentTotalDto> amounts) { this.amounts = amounts; }

    public List<CashSegmentDto> getLots() { return lots; }
    public void setLots(List<CashSegmentDto> lots) { this.lots = lots; }

    public List<DailyMarginDto> getGenerals() { return generals; }
    public void setGenerals(List<DailyMarginDto> generals) { this.generals = generals; }

    public DailyMarginTotalDto getJournal() { return journal; }
    public void setJournal(DailyMarginTotalDto journal) { this.journal = journal; }

    public List<MarginPledgeDto> getSecurities() { return securities; }
    public void setSecurities(List<MarginPledgeDto> securities) { this.securities = securities; }

    public MarginPledgeTotalDto getQuantity() { return quantity; }
    public void setQuantity(MarginPledgeTotalDto quantity) { this.quantity = quantity; }

    @Override
    public String toString() {
        return "GeojitStatementDTO{" +
                "header=" + header +
                ", exchangeCount=" + (exchanges != null ? exchanges.size() : 0) +
                ", positionCount=" + (positions != null ? positions.size() : 0) +
                ", vRecordCount=" + (vRecords != null ? vRecords.size() : 0) +
                ", detailCount=" + (details != null ? details.size() : 0) +
                ", uRecordCount=" + (uRecords != null ? uRecords.size() : 0) +
                '}';
    }
}