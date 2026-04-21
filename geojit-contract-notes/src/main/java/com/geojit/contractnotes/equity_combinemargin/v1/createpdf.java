//package com.geojit.contractnotes.geojit_ContractNote;
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.InputStream;
//import java.security.Security;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import com.geojit.contractnotes.Model.*;
//import com.geojit.contractnotes.utils.HeaderFooterPageEventV2;
//import org.bouncycastle.jce.provider.BouncyCastleProvider;
//import org.json.JSONObject;
//
//import com.amazonaws.services.lambda.runtime.Context;
//import com.amazonaws.services.lambda.runtime.RequestHandler;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//
//import com.geojit.contractnotes.DTO.EquityDtoV2;
//
////import com.geojit.contractnotes.Model.AHeaderTypeModel;
//
//import com.amazonaws.ClientConfiguration;
//import com.amazonaws.services.s3.AmazonS3;
//import com.amazonaws.services.s3.AmazonS3ClientBuilder;
//import com.amazonaws.services.s3.model.ObjectMetadata;
//import com.amazonaws.services.s3.model.PutObjectRequest;
//
//import com.itextpdf.text.BaseColor;
//import com.itextpdf.text.Chunk;
//import com.itextpdf.text.Document;
//import com.itextpdf.text.DocumentException;
//import com.itextpdf.text.Element;
//import com.itextpdf.text.Font;
//import com.itextpdf.text.FontFactory;
//import com.itextpdf.text.PageSize;
//import com.itextpdf.text.Paragraph;
//import com.itextpdf.text.Phrase;
//import com.itextpdf.text.Rectangle;
//import com.itextpdf.text.pdf.BaseFont;
//import com.itextpdf.text.pdf.PdfContentByte;
//import com.itextpdf.text.pdf.PdfPCell;
//import com.itextpdf.text.pdf.PdfPTable;
//import com.itextpdf.text.pdf.PdfPageEvent;
//import com.itextpdf.text.pdf.PdfReader;
//import com.itextpdf.text.pdf.PdfSignatureAppearance;
//import com.itextpdf.text.pdf.PdfStamper;
//import com.itextpdf.text.pdf.PdfWriter;
//import com.itextpdf.text.pdf.security.BouncyCastleDigest;
//import com.itextpdf.text.pdf.security.ExternalDigest;
//import com.itextpdf.text.pdf.security.ExternalSignature;
//import com.itextpdf.text.pdf.security.MakeSignature;
//import com.itextpdf.text.pdf.security.PrivateKeySignature;
//
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.security.GeneralSecurityException;
//import java.security.KeyStore;
//import java.security.PrivateKey;
//import java.security.Provider;
//import java.security.cert.Certificate;
//import java.text.DecimalFormat;
//
//public class createpdf {
//
//    private static List<CustomerModel> customerList = new ArrayList<>();
//    private static List<FooterModelV2> footerList = new ArrayList<>();
//    private static List<FOHeaderTypeModel> foHeaderTypeList = new ArrayList<>();
//    private static List<OHeaderTypeModel> oHeaderTypeList = new ArrayList<>();
//    private static List<DHeaderTypeModel> dHeaderTypeList = new ArrayList<>();
//    private static List<DealingOfficeAddress> dealingOfficeAddressList = new ArrayList<>();
//    private static List<SCapitalHeaderTypeModel> sCapitalHeaderTypeList = new ArrayList<>();
//    private static List<SFuturesHeaderTypeModel> sFuturesHeaderTypeList = new ArrayList<>();
//    private static List<CAHeaderTypeModel> caHeaderTypeList = new ArrayList<>();
//    private static List<SSHeaderTypeModel> ssHeaderTypeModels=new ArrayList<>();
//    private static List<STTHeaderTypeModel> sttHeaderTypeModels= new ArrayList<>();
//    private static List<PHeaderTypeModel> pHeaderTypeModels= new ArrayList<>();
//    private static List<MHeaderTypeModel> mHeaderTypeModels= new ArrayList<>();
//
//}
