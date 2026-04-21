package com.geojit.contractnotes.equity_combinemargin.v1.Model;

import com.fasterxml.jackson.annotation.JsonAlias;

public class DHeaderTypeModel {

    private String partycode;                       // f[0]  TRADE_CODE
    private String headertype;                      // f[1]  D
    private String exchange;                        // f[2]  EXCHANGE
    private String segment;                         // f[3]  SEGMENT
    private String orderno;                         // f[4]  ORDER_NO
    private String order_time;                      // f[5]  ORDER_TIME
    private String trade_no;                        // f[6]  TRADE_NO
    private String trade_time;                      // f[7]  TRADE_TIME
    private String security_contract_description;   // f[8]  CONTRACT_DESC
    private String buy_sell;                        // f[9]  TRADE_TYPE
    private String qty;                             // f[10] ORDER_QTY
    private String gross_rate_fc;                   // f[11] GROSS_RATE_FC
    private String market_rate;                     // f[12] GROSS_RATE
    private String brokerage;                       // f[13] BROKERAGE
    private String net_rate;                        // f[14] NET_RATE_PER_UNIT
    private String closing_rate;                    // f[15] ORDER_CLOSING_RATE
    private String net_total;
    @JsonAlias({"remark", "remarks"})
    private String remark;                          // f[17] REMARKS
    private String exchangeID;                      // f[18] SUBTOTAL_DESC  → PDF calls getExchangeID()
    private String settlementNo;                   // f[19] STTL_NO        → PDF calls getSettlementNo()
    private String settlementdate;                 // f[20] STTL_DATE      → PDF calls getSettlementdate()
    private String segment2;                        // f[21] SEGMENT
    private String broker_code;                     // f[22] BROKER_CODE
    private String isin;
    private String securityname;
    private  String quantity;
    private String price;
    private String buySell;


    public String getPartycode() { return partycode; }
    public void setPartycode(String partycode) { this.partycode = partycode; }

    public String getHeadertype() { return headertype; }
    public void setHeadertype(String headertype) { this.headertype = headertype; }

    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }

    public String getSegment() { return segment; }
    public void setSegment(String segment) { this.segment = segment; }

    public String getOrderno() { return orderno; }
    public void setOrderno(String orderno) { this.orderno = orderno; }

    public String getOrder_time() { return order_time; }
    public void setOrder_time(String order_time) { this.order_time = order_time; }

    public String getTrade_no() { return trade_no; }
    public void setTrade_no(String trade_no) { this.trade_no = trade_no; }

    public String getTrade_time() { return trade_time; }
    public void setTrade_time(String trade_time) { this.trade_time = trade_time; }

    public String getSecurity_contract_description() { return security_contract_description; }
    public void setSecurity_contract_description(String val) { this.security_contract_description = val; }

    public String getBuy_sell() { return buy_sell; }
    public void setBuy_sell(String buy_sell) { this.buy_sell = buy_sell; }

    public String getQty() { return qty; }
    public void setQty(String qty) { this.qty = qty; }

    public String getGross_rate_fc() { return gross_rate_fc; }
    public void setGross_rate_fc(String gross_rate_fc) { this.gross_rate_fc = gross_rate_fc; }

    public String getMarket_rate() { return market_rate; }
    public void setMarket_rate(String market_rate) { this.market_rate = market_rate; }

    public String getBrokerage() { return brokerage; }
    public void setBrokerage(String brokerage) { this.brokerage = brokerage; }

    public String getNet_rate() { return net_rate; }
    public void setNet_rate(String net_rate) { this.net_rate = net_rate; }

    public String getClosing_rate() { return closing_rate; }
    public void setClosing_rate(String closing_rate) { this.closing_rate = closing_rate; }

    public String getNet_total() { return net_total; }
    public void setNet_total(String net_total) { this.net_total = net_total; }

    // PDF calls both getRemark() and getRemarks() — both return same field
    public String getRemark() { return remark; }
    public String getRemarks() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    // PDF calls getExchangeID() → f[18] SUBTOTAL_DESC
    public String getExchangeID() { return exchangeID; }
    public void setExchangeID(String exchangeID) { this.exchangeID = exchangeID; }

    // PDF calls getSettlementNo() → f[19] STTL_NO
    public String getSettlementNo() { return settlementNo; }
    public void setSettlementNo(String settlementNo) { this.settlementNo = settlementNo; }

    // PDF calls getSettlementdate() → f[20] STTL_DATE
    public String getSettlementdate() { return settlementdate; }
    public void setSettlementdate(String settlementdate) { this.settlementdate = settlementdate; }

    public String getSegment2() { return segment2; }
    public void setSegment2(String segment2) { this.segment2 = segment2; }

    public String getBroker_code() { return broker_code; }
    public void setBroker_code(String broker_code) { this.broker_code = broker_code; }
    public String getIsin() {
        return isin; // or return the appropriate field name
    }

    public String getSecurityname() {
        return securityname;
    }

    public String getQuantity() {
        return quantity;
    }

    public String getPrice() {
        return price;
    }

//    public String getBrokerage() {
//        return brokerage;
//    }

    public String getBuySell() {
        return buySell;
    }
    @Override
    public String toString() {
        return "DHeaderTypeModel [partycode=" + partycode + ", exchange=" + exchange +
                ", segment=" + segment + ", orderno=" + orderno +
                ", security_contract_description=" + security_contract_description +
                ", buy_sell=" + buy_sell + ", qty=" + qty +
                ", market_rate=" + market_rate + ", brokerage=" + brokerage +
                ", net_total=" + net_total + ", exchangeID=" + exchangeID +
                ", settlementNo=" + settlementNo + ", settlementdate=" + settlementdate + "]";
    }
}