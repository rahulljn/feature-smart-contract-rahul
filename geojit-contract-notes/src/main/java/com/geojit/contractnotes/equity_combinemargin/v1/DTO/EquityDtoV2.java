package com.geojit.contractnotes.equity_combinemargin.v1.DTO;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.CustomerModel;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.DHeaderTypeModel;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.DealingOfficeAddress;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.OHeaderTypeModel;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.CAHeaderTypeModel;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.SCapitalHeaderTypeModel;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.SFuturesHeaderTypeModel;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.FOHeaderTypeModel;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.SSHeaderTypeModel;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.STTHeaderTypeModel;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.MHeaderTypeModel;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.PHeaderTypeModel;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.CHeaderTypeModel;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.FooterModelV2;

public class EquityDtoV2 {

    private List<CustomerModel> customerList;                   // H type
    private List<DealingOfficeAddress> dealingOfficeAddressList; // A type
    // ✅ CORRECT - Capital F and C to match JSON
    @JsonProperty("sFuturesHeaderTypeList")

    private List<SFuturesHeaderTypeModel> sFuturesHeaderTypeList;
    @JsonProperty("sCapitalHeaderTypeList")

    private List<SCapitalHeaderTypeModel> sCapitalHeaderTypeList;// S type FUTURES
    private List<FOHeaderTypeModel> foHeaderTypeList;           // F type (Footer summary)
    private List<DHeaderTypeModel> dHeaderTypeList;             // D type (Detail trades)
    private List<OHeaderTypeModel> oHeaderTypeList;             // O type
    private List<SSHeaderTypeModel> ssHeaderTypeList;           // SS type (Stock Summary)
    private List<STTHeaderTypeModel> sttHeaderTypeList;         // STT type
    private List<MHeaderTypeModel> mHeaderTypeList;             // M type (Margin)
    private List<PHeaderTypeModel> pHeaderTypeList;             // P type (Portfolio)
    private List<CHeaderTypeModel> cHeaderTypeList;             // C type (Charges)
    private List<CAHeaderTypeModel> caHeaderTypeList;           // CA type
    private List<FooterModelV2> footerList;                     // Footer information

    public List<CustomerModel> getCustomerList() {
        return customerList;
    }

    public void setCustomerList(List<CustomerModel> customerList) {
        this.customerList = customerList;
    }

    public List<DealingOfficeAddress> getDealingOfficeAddressList() {
        return dealingOfficeAddressList;
    }

    public void setDealingOfficeAddressList(List<DealingOfficeAddress> dealingOfficeAddressList) {
        this.dealingOfficeAddressList = dealingOfficeAddressList;
    }

    public List<SFuturesHeaderTypeModel> getSFuturesHeaderTypeList() {
        return sFuturesHeaderTypeList;
    }

    public void setSFuturesHeaderTypeList(List<SFuturesHeaderTypeModel> sFuturesHeaderTypeList) {
        this.sFuturesHeaderTypeList = sFuturesHeaderTypeList;
    }

    public List<SCapitalHeaderTypeModel> getSCapitalHeaderTypeList() {
        return sCapitalHeaderTypeList;
    }

    public void setSCapitalHeaderTypeList(List<SCapitalHeaderTypeModel> sCapitalHeaderTypeList) {
        this.sCapitalHeaderTypeList = sCapitalHeaderTypeList;
    }
    public List<FOHeaderTypeModel> getFoHeaderTypeList() {
        return foHeaderTypeList;
    }

    public void setFoHeaderTypeList(List<FOHeaderTypeModel> foHeaderTypeList) {
        this.foHeaderTypeList = foHeaderTypeList;
    }

    public List<DHeaderTypeModel> getdHeaderTypeList() {
        return dHeaderTypeList;
    }

    public void setdHeaderTypeList(List<DHeaderTypeModel> dHeaderTypeList) {
        this.dHeaderTypeList = dHeaderTypeList;
    }

    public List<OHeaderTypeModel> getoHeaderTypeList() {
        return oHeaderTypeList;
    }

    public void setoHeaderTypeList(List<OHeaderTypeModel> oHeaderTypeList) {
        this.oHeaderTypeList = oHeaderTypeList;
    }

    public List<SSHeaderTypeModel> getSsHeaderTypeList() {
        return ssHeaderTypeList;
    }

    public void setSsHeaderTypeList(List<SSHeaderTypeModel> ssHeaderTypeList) {
        this.ssHeaderTypeList = ssHeaderTypeList;
    }

    public List<STTHeaderTypeModel> getSttHeaderTypeList() {
        return sttHeaderTypeList;
    }

    public void setSttHeaderTypeList(List<STTHeaderTypeModel> sttHeaderTypeList) {
        this.sttHeaderTypeList = sttHeaderTypeList;
    }

    public List<MHeaderTypeModel> getmHeaderTypeList() {
        return mHeaderTypeList;
    }

    public void setmHeaderTypeList(List<MHeaderTypeModel> mHeaderTypeList) {
        this.mHeaderTypeList = mHeaderTypeList;
    }

    public List<PHeaderTypeModel> getpHeaderTypeList() {
        return pHeaderTypeList;
    }

    public void setpHeaderTypeList(List<PHeaderTypeModel> pHeaderTypeList) {
        this.pHeaderTypeList = pHeaderTypeList;
    }

    public List<CHeaderTypeModel> getcHeaderTypeList() {
        return cHeaderTypeList;
    }

    public void setcHeaderTypeList(List<CHeaderTypeModel> cHeaderTypeList) {
        this.cHeaderTypeList = cHeaderTypeList;
    }

    public List<CAHeaderTypeModel> getCaHeaderTypeList() {
        return caHeaderTypeList;
    }

    public void setCaHeaderTypeList(List<CAHeaderTypeModel> caHeaderTypeList) {
        this.caHeaderTypeList = caHeaderTypeList;
    }

    public List<FooterModelV2> getFooterList() {
        return footerList;
    }

    public void setFooterList(List<FooterModelV2> footerList) {
        this.footerList = footerList;
    }

    @Override
    public String toString() {
        return "EquityDtoV2 [" +
                "customerList=" + customerList +
                ", dealingOfficeAddressList=" + dealingOfficeAddressList +
                ", sCapitalHeaderTypeList=" + sCapitalHeaderTypeList +
                ", sFuturesHeaderTypeList=" + sFuturesHeaderTypeList +
                ", foHeaderTypeList=" + foHeaderTypeList +
                ", dHeaderTypeList=" + dHeaderTypeList +
                ", oHeaderTypeList=" + oHeaderTypeList +
                ", ssHeaderTypeList=" + ssHeaderTypeList +
                ", sttHeaderTypeList=" + sttHeaderTypeList +
                ", mHeaderTypeList=" + mHeaderTypeList +
                ", pHeaderTypeList=" + pHeaderTypeList +
                ", cHeaderTypeList=" + cHeaderTypeList +
                ", caHeaderTypeList=" + caHeaderTypeList +
                ", footerList=" + footerList +
                "]";
    }
}