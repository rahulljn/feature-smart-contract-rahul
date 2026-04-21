package com.geojit.contractnotes.equity_combinemargin.v1.Model;

public class FooterModelV2 {

    private String partycode;                       // f[0]  TRADE_CODE
    private String footer_type;                     // f[1]  F
    private String exchange;                        // f[2]  EXCHANGE
    private String segment;                         // f[3]  SEGMENT
    private String instrument;                      // f[4]  INSTRUMENT
    private String pay_in_pay_out_obligation;       // f[5]  PAY_IN_OUT_OBLIGATION  → PDF calls getPay_in_pay_out_obligation()
    private String securities_transaction_tax;      // f[6]  SECURITY_TXN_TAX       → PDF calls getSecurities_transaction_tax()
    private String sgst;                            // f[7]  SGST
    private String cgst;                            // f[8]  CGST
    private String igst;                            // f[9]  IGST
    private String tds;                             // f[10] TDS
    private String exchange_transaction_charges;    // f[11] EXCHANGE_TXN_CHARGES   → PDF calls getExchange_transaction_charges()
    private String sebi_fee;                        // f[12] SEBI_FEE
    private String add_cess;                        // f[13] ADD_CESS
    private String stampduty;                       // f[14] STAMP_DUTY             → PDF calls getStampduty()
    private String net_amount;
    private String payinout;
    private String stt;
    private String exchangecharge;
    private String sebifee;
    private String cess;
    // f[15] NET_AMOUNT

    // These fields are NOT in raw F record but PDF calls them — default "0"
    private String taxable_value_of_supply = "0";   // PDF calls getTaxable_value_of_supply()
    private String turnover_fees = "0";             // PDF calls getTurnover_fees()
    private String auction_and_other_charges = "0"; // PDF calls getAuction_and_other_charges()
    private String ipf_charges = "0";               // PDF calls getIpf_charges()
    private String utt = "0";                       // PDF calls getUtt()
    private String sgstType = "NA";                 // PDF calls getSgstType()
    private String cgstType = "NA";                 // PDF calls getCgstType()

    // ===== f[0] =====
    public String getPartycode() { return partycode; }
    public void setPartycode(String partycode) { this.partycode = partycode; }

    // ===== f[1] =====
    public String getFooter_type() { return footer_type; }
    public void setFooter_type(String footer_type) { this.footer_type = footer_type; }

    // ===== f[2] =====
    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }

    // ===== f[3] =====
    public String getSegment() { return segment; }
    public void setSegment(String segment) { this.segment = segment; }

    // ===== f[4] =====
    public String getInstrument() { return instrument; }
    public void setInstrument(String instrument) { this.instrument = instrument; }

    // ===== f[5] — PDF calls getPay_in_pay_out_obligation() =====
    public String getPay_in_pay_out_obligation() { return pay_in_pay_out_obligation; }
    public void setPay_in_pay_out_obligation(String val) { this.pay_in_pay_out_obligation = val; }

    // ===== f[6] — PDF calls getSecurities_transaction_tax() =====
    public String getSecurities_transaction_tax() { return securities_transaction_tax; }
    public void setSecurities_transaction_tax(String val) { this.securities_transaction_tax = val; }

    // ===== f[7] =====
    public String getSgst() { return sgst; }
    public void setSgst(String sgst) { this.sgst = sgst; }

    // ===== f[8] =====
    public String getCgst() { return cgst; }
    public void setCgst(String cgst) { this.cgst = cgst; }

    // ===== f[9] =====
    public String getIgst() { return igst; }
    public void setIgst(String igst) { this.igst = igst; }

    // ===== f[10] =====
    public String getTds() { return tds; }
    public void setTds(String tds) { this.tds = tds; }

    // ===== f[11] — PDF calls getExchange_transaction_charges() =====
    public String getExchange_transaction_charges() { return exchange_transaction_charges; }
    public void setExchange_transaction_charges(String val) { this.exchange_transaction_charges = val; }

    // ===== f[12] =====
    public String getSebi_fee() { return sebi_fee; }
    public void setSebi_fee(String sebi_fee) { this.sebi_fee = sebi_fee; }

    // ===== f[13] =====
    public String getAdd_cess() { return add_cess; }
    public void setAdd_cess(String add_cess) { this.add_cess = add_cess; }

    // ===== f[14] — PDF calls getStampduty() =====
    public String getStampduty() { return stampduty; }
    public void setStampduty(String stampduty) { this.stampduty = stampduty; }

    // ===== f[15] =====
    public String getNet_amount() { return net_amount; }
    public void setNet_amount(String net_amount) { this.net_amount = net_amount; }

    // ===== NOT in raw file — PDF still calls these, defaults "0" / "NA" =====
    public String getTaxable_value_of_supply() { return taxable_value_of_supply; }
    public void setTaxable_value_of_supply(String val) { this.taxable_value_of_supply = val; }

    public String getTurnover_fees() { return turnover_fees; }
    public void setTurnover_fees(String val) { this.turnover_fees = val; }

    public String getAuction_and_other_charges() { return auction_and_other_charges; }
    public void setAuction_and_other_charges(String val) { this.auction_and_other_charges = val; }

    public String getIpf_charges() { return ipf_charges; }
    public void setIpf_charges(String val) { this.ipf_charges = val; }

    public String getUtt() { return utt; }
    public void setUtt(String val) { this.utt = val; }

    public String getSgstType() { return sgstType; }
    public void setSgstType(String val) { this.sgstType = val; }

    public String getCgstType() { return cgstType; }
    public void setCgstType(String val) { this.cgstType = val; }

    public String getPayinout() {
        return payinout;
    }

    public void setPayinout(String payinout) {
        this.payinout = payinout;
    }

    public String getStt() {
        return stt;
    }

    public void setStt(String stt) {
        this.stt = stt;
    }



    public String getExchangecharge() {
        return exchangecharge;
    }

    public void setExchangecharge(String exchangecharge) {
        this.exchangecharge = exchangecharge;
    }

    public String getSebifee() {
        return sebifee;
    }

    public void setSebifee(String sebifee) {
        this.sebifee = sebifee;
    }

    public String getCess() {
        return cess;
    }

    public void setCess(String cess) {
        this.cess = cess;
    }

    @Override
    public String toString() {
        return "FooterModelV2 [partycode=" + partycode + ", exchange=" + exchange +
                ", segment=" + segment + ", instrument=" + instrument +
                ", pay_in_pay_out_obligation=" + pay_in_pay_out_obligation +
                ", securities_transaction_tax=" + securities_transaction_tax +
                ", sgst=" + sgst + ", cgst=" + cgst + ", igst=" + igst +
                ", exchange_transaction_charges=" + exchange_transaction_charges +
                ", stampduty=" + stampduty + ", net_amount=" + net_amount + "]";
    }
}