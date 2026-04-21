package com.geojit.contractnote.service;

import com.geojit.contractnote.dto.response.ValidationResultResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pre-flight file validator mirroring Split Lambda validation rules exactly.
 *
 * Structure mirrors Split.java processStatementFile exactly:
 *   - Customer boundary: H record OR different customer ID (Split.java line 179)
 *   - Only customers that have an H record are counted (Split.java line 185)
 *   - H validation: field count check inline (Split.java line 204)
 *   - Non-H validation: field count per record type + record-ID match (Split.java validateLine)
 *   - Orphaned customers (no H): not counted, same as Split
 *   - Last customer: only counted if currentCustomerHasHeader (Split.java line 237)
 */
@Slf4j
@Service
public class FileValidationService {

    private static final String DELIMITER = "~";

    // Field counts — exact values from Split.java / Invoke.java
    private static final int H_LEN = 15;
    private static final int E_LEN = 7;
    private static final int P_LEN = 16;
    private static final int V_LEN = 12;
    private static final int D_LEN = 16;
    private static final int O_LEN = 14;
    private static final int F_LEN = 4;
    private static final int N_LEN = 14;
    private static final int M_LEN = 12;
    private static final int T_LEN = 5;
    private static final int U_LEN = 11;
    private static final int C_LEN = 18;
    private static final int R_LEN = 3;
    private static final int A_LEN = 5;
    private static final int L_LEN = 10;
    private static final int G_LEN = 17;
    private static final int J_LEN = 15;
    private static final int K_LEN = 7;
    private static final int Q_LEN = 6;

    public ValidationResultResponse validate(MultipartFile file) throws Exception {
        int totalCustomers   = 0;
        int validCustomers   = 0;
        int invalidCustomers = 0;

        // Track first failing record type per customer — for diagnosis
        Map<String, Integer> invalidReasons = new LinkedHashMap<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8), 65536)) {

            // ── State variables — mirrors Split.java processStatementFile ──────
            String  currentCustomerId         = null;
            boolean currentCustomerValid      = true;
            boolean currentCustomerHasHeader  = false;
            String  currentCustomerFailReason = null;
            int     aCounter                  = 0; // Invoke: count of valid A records for L/A check

            boolean firstLine = true;
            String  line;

            while ((line = reader.readLine()) != null) {
                // BOM removal — first line only (Split.java lines 157-161)
                if (firstLine) {
                    if (line.startsWith("\uFEFF")) line = line.substring(1);
                    firstLine = false;
                }
                if (line.trim().isEmpty()) continue;

                String[] strFields = line.split(DELIMITER, -1);
                if (strFields.length < 2) continue;

                String recordId   = strFields[0];
                String recordType = strFields[1];

                // ── Customer boundary (Split.java line 179) ───────────────────
                boolean isNewCustomer = recordType.equals("H") ||
                        (currentCustomerId != null && !recordId.equals(currentCustomerId));

                if (isNewCustomer && currentCustomerHasHeader) {
                    totalCustomers++;
                    if (currentCustomerValid) {
                        validCustomers++;
                    } else {
                        invalidCustomers++;
                        String reason = currentCustomerFailReason != null ? currentCustomerFailReason : "UNKNOWN";
                        invalidReasons.merge(reason, 1, Integer::sum);
                    }
                }

                if (isNewCustomer) {
                    currentCustomerValid      = true;
                    currentCustomerHasHeader  = false;
                    currentCustomerFailReason = null;
                    aCounter                  = 0;
                }

                // ── H record (Split.java lines 201-215) ──────────────────────
                if (recordType.equals("H")) {
                    currentCustomerHasHeader = true;
                    currentCustomerId        = recordId;
                    currentCustomerValid     = (strFields.length == H_LEN);
                    if (!currentCustomerValid) currentCustomerFailReason = "Invalid header record";
                    aCounter = 0;
                    continue;

                // ── Orphaned customer (Split.java lines 217-224) ──────────────
                } else if (currentCustomerId == null || !recordId.equals(currentCustomerId)) {
                    currentCustomerId         = recordId;
                    currentCustomerValid      = false;
                    currentCustomerFailReason = "Missing header record";
                    continue;
                }

                // ── Non-H record: Split.java validateLine ─────────────────────
                if (!recordId.equals(currentCustomerId)) {
                    currentCustomerValid = false;
                    if (currentCustomerFailReason == null) currentCustomerFailReason = "Invalid record";
                    continue;
                }

                boolean lineValid = validateLine(recordType, strFields.length);
                if (!lineValid && currentCustomerValid) {
                    currentCustomerValid      = false;
                    currentCustomerFailReason = "Invalid record";
                }

                // ── L/A order check ───────────────────────────────────────────
                if (lineValid && recordType.equals("A")) {
                    aCounter++;
                } else if (recordType.equals("L") && strFields.length == L_LEN) {
                    try {
                        int slNo = Integer.parseInt(strFields[2].trim());
                        if (slNo > aCounter) {
                            if (currentCustomerValid) currentCustomerFailReason = "Invalid record ordering";
                            currentCustomerValid = false;
                        }
                    } catch (NumberFormatException e) {
                        if (currentCustomerValid) currentCustomerFailReason = "Invalid record";
                        currentCustomerValid = false;
                    }
                }
            }

            // ── Last customer (Split.java line 237) ───────────────────────────
            if (currentCustomerHasHeader) {
                totalCustomers++;
                if (currentCustomerValid) {
                    validCustomers++;
                } else {
                    invalidCustomers++;
                    String reason = currentCustomerFailReason != null ? currentCustomerFailReason : "UNKNOWN";
                    invalidReasons.merge(reason, 1, Integer::sum);
                }
            }
        }

        log.info("Pre-flight validation complete: total={} valid={} invalid={} reasons={}",
                totalCustomers, validCustomers, invalidCustomers, invalidReasons);

        return ValidationResultResponse.builder()
                .totalCustomers(totalCustomers)
                .validCustomers(validCustomers)
                .invalidCustomers(invalidCustomers)
                .invalidReasons(invalidReasons)
                .build();
    }

    /**
     * Field count validation per record type — mirrors Split.java validateLine switch/case.
     * Returns false for unknown record types (Split.java default case).
     */
    private boolean validateLine(String recordType, int length) {
        return switch (recordType) {
            case "E" -> length == E_LEN;
            case "P" -> length == P_LEN;
            case "V" -> length == V_LEN;
            case "D" -> length == D_LEN;
            case "O" -> length == O_LEN;
            case "F" -> length == F_LEN;
            case "N" -> length == N_LEN;
            case "M" -> length == M_LEN;
            case "T" -> length == T_LEN;
            case "U" -> length == U_LEN;
            case "C" -> length == C_LEN;
            case "R" -> length == R_LEN;
            case "A" -> length == A_LEN;
            case "L" -> length == L_LEN;
            case "G" -> length == G_LEN;
            case "J" -> length == J_LEN;
            case "K" -> length == K_LEN;
            case "Q" -> length == Q_LEN;
            default  -> false; // unknown record type → invalid (Split.java default case)
        };
    }
}
