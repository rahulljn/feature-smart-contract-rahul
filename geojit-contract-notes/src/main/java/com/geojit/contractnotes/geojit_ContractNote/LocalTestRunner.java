package com.geojit.contractnotes.geojit_ContractNote;//package com.geojit.contractnotes.geojit_ContractNote;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.geojit.contractnotes.DTO.GeojitStatementDTO;
//
//import java.io.File;
//import java.io.InputStream;
//
//public class LocalTestRunner {
//
//    public static void main(String[] args) throws Exception {
//        System.out.println("═══════════════════════════════════════════════════");
//        System.out.println("  GEOJIT PDF LOCAL RUNNER - Bypassing AWS Lambda  ");
//        System.out.println("═══════════════════════════════════════════════════");
//
//        ObjectMapper mapper = new ObjectMapper();
//        GeojitStatementDTO dto;
//
//        if (args.length > 0) {
//            System.out.println("→ Loading JSON from arg: " + args[0]);
//            dto = mapper.readValue(new File(args[0]), GeojitStatementDTO.class);
//
//        } else {
//            InputStream stream = LocalTestRunner.class
//                    .getClassLoader()
//                    .getResourceAsStream("sample_payload.json");
//
//            if (stream != null) {
//                System.out.println("→ Loading JSON from classpath: sample_payload.json");
//                dto = mapper.readValue(stream, GeojitStatementDTO.class);
//
//            } else {
//                String classesPath = LocalTestRunner.class
//                        .getProtectionDomain().getCodeSource().getLocation().getPath();
//                File projectRoot = new File(classesPath).getParentFile().getParentFile();
//                File jsonFile    = new File(projectRoot, "src/main/resources/sample_payload.json");
//                System.out.println("→ Fallback path: " + jsonFile.getAbsolutePath());
//                if (!jsonFile.exists()) {
//                    throw new java.io.FileNotFoundException(
//                            "\n❌ sample_payload.json NOT found!\n" +
//                                    "   Place it at: src/main/resources/sample_payload.json\n"
//                    );
//                }
//                dto = mapper.readValue(jsonFile, GeojitStatementDTO.class);
//            }
//        }
//
//        System.out.println("✓ JSON loaded");
//
//        DynamicValuePdf generator = new DynamicValuePdf();
//        generator.loadModelsFromDTO(dto);   // must be public in DynamicValuePdf
//        generator.validateData();           // must be public in DynamicValuePdf
//
//        File pdf = generator.generatePDF(); // no parameters — new version takes none
//
//        System.out.println("═══════════════════════════════════════════════════");
//        System.out.println("✓ PDF saved at: " + pdf.getAbsolutePath());
//        System.out.println("═══════════════════════════════════════════════════");
//    }
//}