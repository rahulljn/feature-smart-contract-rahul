package com.geojit.contractnotes.equity_combinemargin.v2.DTO;

import com.geojit.contractnotes.equity_combinemargin.v2.MODEL.CNAddressDTO;
import com.geojit.contractnotes.equity_combinemargin.v2.MODEL.CNChargesDTO;
import com.geojit.contractnotes.equity_combinemargin.v2.MODEL.CNFinancialSummaryDTO;
import com.geojit.contractnotes.equity_combinemargin.v2.MODEL.CNHeaderDTO;
import com.geojit.contractnotes.equity_combinemargin.v2.MODEL.CNMarginStatementDTO;
import com.geojit.contractnotes.equity_combinemargin.v2.MODEL.CNNetObligationDTO;
import com.geojit.contractnotes.equity_combinemargin.v2.MODEL.CNPledgeSecuritiesDTO;
import com.geojit.contractnotes.equity_combinemargin.v2.MODEL.CNSTTDTO;
import com.geojit.contractnotes.equity_combinemargin.v2.MODEL.CNScripSummaryDTO;
import com.geojit.contractnotes.equity_combinemargin.v2.MODEL.CNSecuritySummaryDTO;
import com.geojit.contractnotes.equity_combinemargin.v2.MODEL.CNTradeDetailsDTO;

import java.util.List;

public class CNContractNoteDTO {
	
	List<CNHeaderDTO> headerList;
	List<CNAddressDTO> addressList;
	List<CNSecuritySummaryDTO> securitySummaryList;
	List<CNFinancialSummaryDTO> financialSummaryList;
	List<CNTradeDetailsDTO> tradeDetailsList;
	List<CNNetObligationDTO> netObligationList;
	List<CNScripSummaryDTO> scripSummaryList;
	List<CNSTTDTO> sttList;
	List<CNMarginStatementDTO> marginStatementList;
	List<CNPledgeSecuritiesDTO> pledgeSecuritiesList;
	List<CNChargesDTO> chargesList;
	
	public List<CNHeaderDTO> getHeaderList() {
		return headerList;
	}
	public void setHeaderList(List<CNHeaderDTO> headerList) {
		this.headerList = headerList;
	}
	public List<CNAddressDTO> getAddressList() {
		return addressList;
	}
	public void setAddressList(List<CNAddressDTO> addressList) {
		this.addressList = addressList;
	}
	public List<CNSecuritySummaryDTO> getSecuritySummaryList() {
		return securitySummaryList;
	}
	public void setSecuritySummaryList(List<CNSecuritySummaryDTO> securitySummaryList) {
		this.securitySummaryList = securitySummaryList;
	}
	public List<CNFinancialSummaryDTO> getFinancialSummaryList() {
		return financialSummaryList;
	}
	public void setFinancialSummaryList(List<CNFinancialSummaryDTO> financialSummaryList) {
		this.financialSummaryList = financialSummaryList;
	}
	public List<CNTradeDetailsDTO> getTradeDetailsList() {
		return tradeDetailsList;
	}
	public void setTradeDetailsList(List<CNTradeDetailsDTO> tradeDetailsList) {
		this.tradeDetailsList = tradeDetailsList;
	}
	public List<CNNetObligationDTO> getNetObligationList() {
		return netObligationList;
	}
	public void setNetObligationList(List<CNNetObligationDTO> netObligationList) {
		this.netObligationList = netObligationList;
	}
	public List<CNScripSummaryDTO> getScripSummaryList() {
		return scripSummaryList;
	}
	public void setScripSummaryList(List<CNScripSummaryDTO> scripSummaryList) {
		this.scripSummaryList = scripSummaryList;
	}
	public List<CNSTTDTO> getSttList() {
		return sttList;
	}
	public void setSttList(List<CNSTTDTO> sttList) {
		this.sttList = sttList;
	}
	public List<CNMarginStatementDTO> getMarginStatementList() {
		return marginStatementList;
	}
	public void setMarginStatementList(List<CNMarginStatementDTO> marginStatementList) {
		this.marginStatementList = marginStatementList;
	}
	public List<CNPledgeSecuritiesDTO> getPledgeSecuritiesList() {
		return pledgeSecuritiesList;
	}
	public void setPledgeSecuritiesList(List<CNPledgeSecuritiesDTO> pledgeSecuritiesList) {
		this.pledgeSecuritiesList = pledgeSecuritiesList;
	}
	public List<CNChargesDTO> getChargesList() {
		return chargesList;
	}
	public void setChargesList(List<CNChargesDTO> chargesList) {
		this.chargesList = chargesList;
	}
	
	@Override
	public String toString() {
		return "CNContractNoteDTO [headerList=" + headerList + ", addressList=" + addressList
				+ ", securitySummaryList=" + securitySummaryList + ", financialSummaryList=" + financialSummaryList
				+ ", tradeDetailsList=" + tradeDetailsList + ", netObligationList=" + netObligationList
				+ ", scripSummaryList=" + scripSummaryList + ", sttList=" + sttList + ", marginStatementList="
				+ marginStatementList + ", pledgeSecuritiesList=" + pledgeSecuritiesList + ", chargesList="
				+ chargesList + "]";
	}
}