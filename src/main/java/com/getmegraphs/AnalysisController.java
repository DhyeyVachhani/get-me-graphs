package com.getmegraphs;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/analysis")
public class AnalysisController {
    private static final Logger logger = LoggerFactory.getLogger(AnalysisController.class);
    
    private final CloudWatchChartService chartService;
    private final AIAnalysisService aiAnalysisService;

    public AnalysisController(CloudWatchChartService chartService, AIAnalysisService aiAnalysisService) {
        this.chartService = chartService;
        this.aiAnalysisService = aiAnalysisService;
    }

    @PostMapping("/comprehensive-report")
    public String generateAndAnalyzeComprehensiveReport(
            @RequestParam(required = false) String clusterName,    // For Kafka metrics
            @RequestParam(required = false) String dbInstanceIdentifier, // For RDS metrics
            @RequestParam String startTime, // ISO 8601, e.g. 2024-06-01T00:00:00Z
            @RequestParam String endTime,   // ISO 8601, e.g. 2024-06-01T23:59:59Z
            @RequestParam(required = false) String analysisType, // performance, anomalies, capacity, or custom
            @RequestParam(required = false) String customPrompt   // Required if analysisType is "custom"
    ) {
        try {
            // First generate the comprehensive PDF report
            String pdfPath = chartService.generatePDFReport(clusterName, dbInstanceIdentifier, startTime, endTime);
            
            // Then analyze it with AI
            String analysisResult;
            
            if ("custom".equalsIgnoreCase(analysisType) && customPrompt != null && !customPrompt.trim().isEmpty()) {
                analysisResult = aiAnalysisService.analyzeReportWithCustomPrompt(pdfPath, customPrompt);
            } else if ("stability".equalsIgnoreCase(analysisType)) {
                analysisResult = aiAnalysisService.analyzeReportForStabilityPrompt(pdfPath);
            } else {
                // Default to performance analysis
                analysisResult = aiAnalysisService.analyzeReportForPerformanceInsights(pdfPath);
            }
            
            return "PDF Report generated at: " + pdfPath + "\n\n" +
                   "=== AI ANALYSIS RESULTS ===\n" + analysisResult;

        } catch (Exception e) {
            logger.error("Error generating and analyzing comprehensive report for cluster: {}, DB instance: {}", clusterName, dbInstanceIdentifier, e);
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/existing-report")
    public String analyzeExistingReport(
            @RequestParam String pdfPath,
            @RequestParam(required = false) String analysisType, // performance, anomalies, capacity, or custom
            @RequestParam(required = false) String customPrompt   // Required if analysisType is "custom"
    ) {
        try {
            String analysisResult;
            
            if ("custom".equalsIgnoreCase(analysisType) && customPrompt != null && !customPrompt.trim().isEmpty()) {
                analysisResult = aiAnalysisService.analyzeReportWithCustomPrompt(pdfPath, customPrompt);
            } else if ("stability".equalsIgnoreCase(analysisType)) {
                analysisResult = aiAnalysisService.analyzeReportForStabilityPrompt(pdfPath);
            } else {
                // Default to performance analysis
                analysisResult = aiAnalysisService.analyzeReportForPerformanceInsights(pdfPath);
            }

            return "=== AI ANALYSIS RESULTS ===\n" + analysisResult;

        } catch (Exception e) {
            logger.error("Error analyzing existing report: {}", pdfPath, e);
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/performance-insights")
    public String generatePerformanceInsights(
            @RequestParam(required = false) String clusterName,
            @RequestParam(required = false) String dbInstanceIdentifier,
            @RequestParam String startTime,
            @RequestParam String endTime
    ) {
        try {
            String pdfPath = chartService.generatePDFReport(clusterName, dbInstanceIdentifier, startTime, endTime);
            String analysisResult = aiAnalysisService.analyzeReportForPerformanceInsights(pdfPath);
            
            return "PDF Report: " + pdfPath + "\n\n=== PERFORMANCE INSIGHTS ===\n" + analysisResult;
        } catch (Exception e) {
            logger.error("Error generating performance insights for cluster: {}, DB instance: {}", clusterName, dbInstanceIdentifier, e);
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/custom-analysis")
    public String generateCustomAnalysis(
            @RequestParam(required = false) String clusterName,
            @RequestParam(required = false) String dbInstanceIdentifier,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam String customPrompt
    ) {
        try {
            String pdfPath = chartService.generatePDFReport(clusterName, dbInstanceIdentifier, startTime, endTime);
            String analysisResult = aiAnalysisService.analyzeReportWithCustomPrompt(pdfPath, customPrompt);
            
            return "PDF Report: " + pdfPath + "\n\n=== CUSTOM ANALYSIS ===\n" + analysisResult;
        } catch (Exception e) {
            logger.error("Error generating custom analysis for cluster: {}, DB instance: {}", clusterName, dbInstanceIdentifier, e);
            return "Error: " + e.getMessage();
        }
    }


    @PostMapping("/stability")
    public String generateStabilityAnalysis(
            @RequestParam(required = false) String clusterName,
            @RequestParam(required = false) String dbInstanceIdentifier,
            @RequestParam String startTime,
            @RequestParam String endTime
    ) {
        try {
            // Generate report directory with charts and vector data
            String reportDir = chartService.generateComprehensiveReport(
                clusterName, dbInstanceIdentifier, startTime, endTime);
            
            // Read the vector data file
            String vectorDataFile = reportDir + "/metrics_vector_data.json";
            java.nio.file.Path vectorDataPath = java.nio.file.Paths.get(vectorDataFile);
            
            if (java.nio.file.Files.exists(vectorDataPath)) {
                String vectorData = java.nio.file.Files.readString(vectorDataPath);
                
                // Analyze the vector data and extract any generated code
                String analysisResult = aiAnalysisService.analyzeVectorDataForStabilityWithCodeExtraction(vectorData, reportDir);
                
                return "Report Directory: " + reportDir + "\n" +
                       "Vector Data File: " + vectorDataFile + "\n" +
                       "Generated Files: Check directory for Java visualization program, compiled charts, and JSON data\n\n" +
                       "=== STABILITY ANALYSIS (Based on Vector Data) ===\n" + analysisResult;
            } else {
                // Fallback to PDF analysis if vector data is not available
                String pdfPath = chartService.generatePDFReport(clusterName, dbInstanceIdentifier, startTime, endTime);
                String analysisResult = aiAnalysisService.analyzeReportForStabilityPrompt(pdfPath);
                
                return "PDF Report: " + pdfPath + "\n" +
                       "Note: Vector data not available, used PDF analysis\n\n" +
                       "=== STABILITY ANALYSIS (Based on PDF) ===\n" + analysisResult;
            }

        } catch (Exception e) {
            logger.error("Error generating stability analysis for cluster: {}, DB instance: {}", clusterName, dbInstanceIdentifier, e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Endpoint to get raw metric vector data in JSON format for direct AI analysis
     * This is perfect for ChatGPT/LLMs that cannot process visual charts but can analyze numerical data
     */
    @GetMapping("/metrics-vector-data")
    public ResponseEntity<String> getMetricsVectorData(
            @RequestParam(required = false) String clusterName,
            @RequestParam(required = false) String dbInstanceIdentifier,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        
        try {
            // Generate report directory with charts and vector data
            String reportDir = chartService.generateComprehensiveReport(
                clusterName, dbInstanceIdentifier, startTime, endTime);
            
            // Read the vector data file
            String vectorDataFile = reportDir + "/metrics_vector_data.json";
            java.nio.file.Path vectorDataPath = java.nio.file.Paths.get(vectorDataFile);
            
            if (java.nio.file.Files.exists(vectorDataPath)) {
                String vectorData = java.nio.file.Files.readString(vectorDataPath);
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                
                return ResponseEntity.ok()
                    .headers(headers)
                    .body(vectorData);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Vector data file not found. This usually means AWS credentials are invalid or expired.\"}");
            }
            
        } catch (Exception e) {
            logger.error("Error generating metrics vector data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("{\"error\": \"Failed to generate metrics vector data: " + e.getMessage() + "\"}");
        }
    }
}
