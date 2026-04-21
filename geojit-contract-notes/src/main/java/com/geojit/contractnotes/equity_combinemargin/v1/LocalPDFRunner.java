//package com.geojit.contractnotes.geojit_ContractNote;
//
//import com.geojit.contractnotes.Model.*;
//import java.util.*;
//
//public class LocalPDFRunner {
//
//    public static void main(String[] args) {
//        try {
//
//            System.out.println("🚀 Running Geojit PDF locally...");
//
//            DynamicValuePDF pdfGenerator = new DynamicValuePDF();
//
//            // ---- Create Dummy Test Data ----
//
//            CustomerModel customer = new CustomerModel();
//            customer.setContractNo("TEST12345");
//            customer.setName("Shiv Developer");
//            customer.setTransactionDate("15.02.2026");
//            customer.setClientCode("UCC001");
//            customer.setMobileNo("9999999999");
//            customer.setPanNo("ABCDE1234F");
//            customer.setGstNo("32ABCDE1234F1Z5");
//            customer.setAddress1("Test Address Line 1");
//            customer.setAddress2("Test Address Line 2");
//
//            DealingOfficeAddress office = new DealingOfficeAddress();
//            office.setDealingAddress("Palarivattom, Kochi");
//
//            List<DHeaderTypeModel> equityList = new ArrayList<>();
//
//            DHeaderTypeModel equity = new DHeaderTypeModel();
//            equity.setExchange("NSE");
//            equity.setSegment("CAPITAL");
////            equity.getIsin("INE123A01016");
////            equity.setSecurityname("TCS LTD");
////            equity.setBuySell("B");
////            equity.setQuantity("10");      // or 10 if Integer
////            equity.setPrice("3500");       // or 3500.0 if Double
//            equity.setBrokerage("20");     // or 20.0 if Double
//
//
//            equityList.add(equity);
//
//            // Empty lists for others
//            List<FooterModelV2> footerList = new ArrayList<>();
//            List<FOHeaderTypeModel> foList = new ArrayList<>();
//            List<OHeaderTypeModel> oList = new ArrayList<>();
//            List<SCapitalHeaderTypeModel> scList = new ArrayList<>();
//            List<SFuturesHeaderTypeModel> sfList = new ArrayList<>();
//            List<CAHeaderTypeModel> caList = new ArrayList<>();
//            List<SSHeaderTypeModel> ssList = new ArrayList<>();
//            List<STTHeaderTypeModel> sttList = new ArrayList<>();
//            List<PHeaderTypeModel> pList = new ArrayList<>();
//            List<MHeaderTypeModel> mList = new ArrayList<>();
//
//            pdfGenerator.generatePDF(
//                    customer,
//                    office,
//                    footerList,
//                    foList,
//                    oList,
//                    equityList,
//                    scList,
//                    sfList,
//                    caList,
//                    ssList,
//                    sttList,
//                    pList,
//                    mList
//            );
//
//            System.out.println("✅ PDF Generated Successfully in Project Folder");
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}
