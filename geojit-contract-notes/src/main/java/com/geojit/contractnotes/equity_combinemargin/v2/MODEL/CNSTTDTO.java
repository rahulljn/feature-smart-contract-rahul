package com.geojit.contractnotes.equity_combinemargin.v2.MODEL;

public class CNSTTDTO {
	
	String tradeCode;
	String recordType; // STT
	String securityDescription;
	String exchange;
	String segment;
	
	// For Cash Transactions
	String purchaseQty;
	String purchasePrice;
	String purchaseValue;
	String purchaseStt;
	String saleQty;
	String salePrice;
	String saleValue;
	String saleStt;
	String qty;
	String price;
	String value;
	String stt;
	String totalStt;
	
	// For Derivative Transactions
	String expDate;
	String futureSale;
	String futureStt;
	String optionSale;
	String optionStt;
	
	public String getTradeCode() {
		return tradeCode;
	}
	public void setTradeCode(String tradeCode) {
		this.tradeCode = tradeCode;
	}
	public String getRecordType() {
		return recordType;
	}
	public void setRecordType(String recordType) {
		this.recordType = recordType;
	}
	public String getSecurityDescription() {
		return securityDescription;
	}
	public void setSecurityDescription(String securityDescription) {
		this.securityDescription = securityDescription;
	}
	public String getExchange() {
		return exchange;
	}
	public void setExchange(String exchange) {
		this.exchange = exchange;
	}
	public String getSegment() {
		return segment;
	}
	public void setSegment(String segment) {
		this.segment = segment;
	}
	public String getPurchaseQty() {
		return purchaseQty;
	}
	public void setPurchaseQty(String purchaseQty) {
		this.purchaseQty = purchaseQty;
	}
	public String getPurchasePrice() {
		return purchasePrice;
	}
	public void setPurchasePrice(String purchasePrice) {
		this.purchasePrice = purchasePrice;
	}
	public String getPurchaseValue() {
		return purchaseValue;
	}
	public void setPurchaseValue(String purchaseValue) {
		this.purchaseValue = purchaseValue;
	}
	public String getPurchaseStt() {
		return purchaseStt;
	}
	public void setPurchaseStt(String purchaseStt) {
		this.purchaseStt = purchaseStt;
	}
	public String getSaleQty() {
		return saleQty;
	}
	public void setSaleQty(String saleQty) {
		this.saleQty = saleQty;
	}
	public String getSalePrice() {
		return salePrice;
	}
	public void setSalePrice(String salePrice) {
		this.salePrice = salePrice;
	}
	public String getSaleValue() {
		return saleValue;
	}
	public void setSaleValue(String saleValue) {
		this.saleValue = saleValue;
	}
	public String getSaleStt() {
		return saleStt;
	}
	public void setSaleStt(String saleStt) {
		this.saleStt = saleStt;
	}
	public String getQty() {
		return qty;
	}
	public void setQty(String qty) {
		this.qty = qty;
	}
	public String getPrice() {
		return price;
	}
	public void setPrice(String price) {
		this.price = price;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public String getStt() {
		return stt;
	}
	public void setStt(String stt) {
		this.stt = stt;
	}
	public String getTotalStt() {
		return totalStt;
	}
	public void setTotalStt(String totalStt) {
		this.totalStt = totalStt;
	}
	public String getExpDate() {
		return expDate;
	}
	public void setExpDate(String expDate) {
		this.expDate = expDate;
	}
	public String getFutureSale() {
		return futureSale;
	}
	public void setFutureSale(String futureSale) {
		this.futureSale = futureSale;
	}
	public String getFutureStt() {
		return futureStt;
	}
	public void setFutureStt(String futureStt) {
		this.futureStt = futureStt;
	}
	public String getOptionSale() {
		return optionSale;
	}
	public void setOptionSale(String optionSale) {
		this.optionSale = optionSale;
	}
	public String getOptionStt() {
		return optionStt;
	}
	public void setOptionStt(String optionStt) {
		this.optionStt = optionStt;
	}
	
	@Override
	public String toString() {
		return "CNSTTDTO [tradeCode=" + tradeCode + ", recordType=" + recordType + ", securityDescription="
				+ securityDescription + ", exchange=" + exchange + ", segment=" + segment + ", purchaseQty="
				+ purchaseQty + ", purchasePrice=" + purchasePrice + ", purchaseValue=" + purchaseValue
				+ ", purchaseStt=" + purchaseStt + ", saleQty=" + saleQty + ", salePrice=" + salePrice + ", saleValue="
				+ saleValue + ", saleStt=" + saleStt + ", qty=" + qty + ", price=" + price + ", value=" + value
				+ ", stt=" + stt + ", totalStt=" + totalStt + ", expDate=" + expDate + ", futureSale=" + futureSale
				+ ", futureStt=" + futureStt + ", optionSale=" + optionSale + ", optionStt=" + optionStt + "]";
	}
}