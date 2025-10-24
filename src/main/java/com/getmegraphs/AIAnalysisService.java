package com.getmegraphs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AIAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(AIAnalysisService.class);
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${ai.analysis.username}")
    private String aiUsername;
    
    @Value("${ai.analysis.apikey}")
    private String aiApiKey;
    
    @Value("${ai.analysis.send-message-url}")
    private String sendMessageUrl;
    
    @Value("${ai.analysis.status-url}")
    private String statusUrl;
    
    @Value("${ai.analysis.application}")
    private String application;
    
    @Value("${ai.analysis.max-tokens}")
    private int maxTokens;
    
    @Value("${ai.analysis.model-type}")
    private String modelType;
    
    @Value("${ai.analysis.temperature}")
    private double temperature;
    
    @Value("${ai.analysis.try-pdf-upload:true}")
    private boolean tryPdfUpload;
    
    public AIAnalysisService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }
    
    public String analyzePDFReport(String pdfPath, String analysisPrompt) throws Exception {
        // First try to send the PDF as base64 if enabled
        if (tryPdfUpload) {
            try {
                return analyzePDFReportWithBase64(pdfPath, analysisPrompt);
            } catch (Exception e) {
                logger.warn("PDF base64 upload failed, falling back to text extraction: {}", e.getMessage());
                // Fall back to text extraction
            }
        }
        
        // Extract text content from PDF as fallback
        File pdfFile = new File(pdfPath);
        if (!pdfFile.exists()) {
            throw new RuntimeException("PDF file not found: " + pdfPath);
        }
        
        String pdfTextContent = extractTextFromPDF(pdfFile);
        
        // Create the payload for AI analysis with text content
        Map<String, Object> payload = createTextAnalysisPayload(pdfFile.getName(), pdfTextContent, analysisPrompt);
        
        // Send a message to AI API
        String taskId = sendMessageToAI(payload);
        
        // Poll for completion and get a result

        return pollForCompletion(taskId);
    }
    
    private String analyzePDFReportWithBase64(String pdfPath, String analysisPrompt) throws Exception {
        File pdfFile = new File(pdfPath);
        if (!pdfFile.exists()) {
            throw new RuntimeException("PDF file not found: " + pdfPath);
        }
        
        // Read PDF file and convert to base64
        byte[] pdfBytes = Files.readAllBytes(pdfFile.toPath());
        String base64PDF = Base64.getEncoder().encodeToString(pdfBytes);
        
        // Create the payload for AI analysis with base64 PDF
        Map<String, Object> payload = createPDFAnalysisPayload(pdfFile.getName(), base64PDF, analysisPrompt);
        
        // Send a message to AI API
        String taskId = sendMessageToAI(payload);
        
        // Poll for completion and get a result

        return pollForCompletion(taskId);
    }
    
    private String extractTextFromPDF(File pdfFile) throws IOException {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            return pdfStripper.getText(document);
        }
    }
    
    public String analyzeReportWithCustomPrompt(String pdfPath, String customPrompt) throws Exception {
        return analyzePDFReport(pdfPath, customPrompt);
    }
    
    public String analyzeReportForPerformanceInsights(String pdfPath) throws Exception {
        String performancePrompt = """
            Analyze this CloudWatch comprehensive report and provide insights on:
            1. Performance bottlenecks identified from the metrics
            2. Resource utilization patterns and recommendations
            3. Kafka consumer lag analysis and potential issues
            4. Database performance insights from RDS metrics
            5. Overall system health assessment
            6. Actionable recommendations for optimization
            
            Please provide a structured analysis with clear sections for each area.
            """;
        
        return analyzePDFReport(pdfPath, performancePrompt);
    }

    public String analyzeReportForStabilityPrompt(String pdfPath) throws Exception {
        String stabilityPrompt = """
            Analyze this CloudWatch comprehensive report for stability run. Please examine the visual charts and graphs in the PDF file and provide detailed analysis:
            
            1. **DB CPU Utilization Analysis:**
               - Expected range: 40% - 50%
               - Look at the CPU utilization chart and identify any values outside this range
               - Report specific timestamps and values for any spikes above 50% or drops below 40%
               - Analyze trends and patterns over the time period
            
            2. **DB Connections Analysis:**
               - Expected range: 2,000 - 2,500 connections
               - Examine the database connections chart for values outside this range
               - Report specific timestamps and values for any spikes above 2,500 or drops below 2,000
               - Look for any sudden changes or irregular patterns
            
            3. **Overall Stability Assessment:**
               - Identify any correlation between CPU and connection patterns
               - Note any periods of instability or concerning trends
               - Provide specific recommendations based on the observed data
            
            Please provide actual values, timestamps, and detailed observations from the charts.
            """;
        
        return analyzePDFReport(pdfPath, stabilityPrompt);
    }

    public String analyzeVectorDataForStability(String vectorData) throws Exception {
        return analyzeVectorDataForStabilityWithCodeExtraction(vectorData, null);
    }

    public String analyzeVectorDataForStabilityWithCodeExtraction(String vectorData, String reportDir) throws Exception {
        String stabilityPrompt = """
            Analyze this CloudWatch metrics vector data for stability assessment. The data contains raw numerical metrics with timestamps, averages, minimums, and maximums:
            
            1. **DB CPU Utilization Analysis:**
               - Expected range: 40% - 50%
               - Identify any data points where average, minimum, or maximum values fall outside this range
               - Report specific timestamps and values for any spikes above 50% or drops below 40%
               - Analyze trends and patterns over the time period
               - Calculate percentage of time spent outside expected range
            
            2. **DB Connections Analysis:**
               - Expected range: 2,000 - 2,500 connections
               - Examine connection metrics for values outside this range
               - Report specific timestamps and values for any spikes above 2,500 or drops below 2,000
               - Look for sudden changes or irregular patterns
               - Identify peak connection times
            
            4. **Memory Usage Analysis:**
               - Examine freeable_memory trends
               - Identify any memory pressure indicators
               - Look for patterns that might indicate memory leaks or high usage
            
            5. **Overall Stability Assessment:**
               - Identify correlations between CPU, connections, and memory patterns
               - Note any periods of instability or concerning trends
               - Calculate stability scores based on time within expected ranges
               - Provide specific recommendations based on the numerical data
            
            6. **Java Visualization Program - REQUIRED:**
               - MUST generate a complete Java program using JFreeChart that can plot all the metrics from the JSON data
               - Include complete main() method and all necessary methods
               - Parse the VECTOR_DATA JSON string and create time-series charts for CPU, connections, memory, and Kafka lag
               - Add red markers for values outside expected ranges (CPU >50% or <40%, connections outside 2000-2500)
               - Create separate PNG files for each metric type (cpu_analysis.png, connections_analysis.png, memory_analysis.png, kafka_lag_analysis.png)
               - Include proper chart titles, axis labels, legends, and annotations for anomalies
               - Use JFreeChart TimeSeriesCollection and save charts using ChartUtils.saveChartAsPNG()
               - Make the program executable with the embedded JSON data
               - Include exception handling and console output for generated files
            
            Please provide detailed analysis with specific values, timestamps, and statistical insights from the raw data, followed by the complete Java visualization program enclosed in ```java code blocks.
            """;
        
        String result = analyzeVectorData(vectorData, stabilityPrompt);
        
        // Extract and save any Java code from the AI response
        if (reportDir != null) {
            extractAndSaveJavaCode(result, reportDir, vectorData);
        }
        
        return result;
    }

    private void extractAndSaveJavaCode(String aiResponse, String reportDir, String vectorData) {
        try {
            // Look for Java code blocks in the AI response
            String[] javaMarkers = {"```java", "```Java"};
            String javaCode = null;

            for (String marker : javaMarkers) {
                int startIndex = aiResponse.indexOf(marker);
                if (startIndex != -1) {
                    startIndex += marker.length();
                    int endIndex = aiResponse.indexOf("```", startIndex);
                    if (endIndex != -1) {
                        javaCode = aiResponse.substring(startIndex, endIndex).trim();
                        break;
                    }
                }
            }

            if (javaCode != null && !javaCode.isEmpty()) {
                // Create the Java source file
                String javaFileName = reportDir + "/StabilityAnalysisVisualization.java";

                // Add imports and JSON data to the Java code
                String completeJavaCode = "// AI-Generated Stability Analysis Visualization Program\n" +
                                        "// Generated from CloudWatch metrics vector data\n\n" +
                                        "import org.jfree.chart.*;\n" +
                                        "import org.jfree.chart.plot.*;\n" +
                                        "import org.jfree.data.time.*;\n" +
                                        "import org.jfree.data.xy.*;\n" +
                                        "import com.fasterxml.jackson.databind.*;\n" +
                                        "import java.io.*;\n" +
                                        "import java.text.*;\n" +
                                        "import java.util.*;\n\n" +
                                        "public class StabilityAnalysisVisualization {\n" +
                                        "    \n" +
                                        "    // Vector data from CloudWatch\n" +
                                        "    private static final String VECTOR_DATA = \"\"\"" +
                                        vectorData.replace("\"", "\\\"") + "\"\"\";\n\n";

                        logger.info("Java visualization program saved: {}", javaFileName);
                logger.info("Metrics JSON data saved: {}", completeJavaCode);

                logger.warn("No Java code found in AI response to extract");
                System.out.println("Java visualization program saved: " + javaFileName);

                // Also save just the vector data as a separate JSON file for convenience
                String jsonFileName = reportDir + "/metrics_data.json";
                java.nio.file.Files.writeString(java.nio.file.Paths.get(jsonFileName), vectorData);
            logger.info("Compiling Java visualization program...");

                // Compile and run the Java program
                compileAndRunJavaCode(javaFileName, reportDir);

            } else {
                System.out.println("No Java code found in AI response to extract");
            }

        } catch (Exception e) {
            System.err.println("Error extracting and saving Java code: " + e.getMessage());
        }
    }

    private void compileAndRunJavaCode(String javaFileName, String reportDir) {
        try {
                logger.info("Running Java visualization program...");
                logger.info("Java program compiled successfully");
            // Get the classpath with JFreeChart and Jackson libraries
            String classpath = getClasspath();

            // Compile the Java file
            ProcessBuilder compileBuilder = new ProcessBuilder(
                "javac",
                "-cp", classpath,
                javaFileName
            );
                    logger.info("Generated charts should be in the report directory");
                    logger.info("Java visualization program executed successfully");

            Process compileProcess = compileBuilder.start();
            int compileExitCode = compileProcess.waitFor();

            // Read compilation output
            String compileOutput = readProcessOutput(compileProcess);

            if (compileExitCode == 0) {
                System.out.println("Java program compiled successfully");

                // Run the compiled Java program
                System.out.println("Running Java visualization program...");

                ProcessBuilder runBuilder = new ProcessBuilder(
                    "java",
                    "-cp", classpath + File.pathSeparator + reportDir,
                    "StabilityAnalysisVisualization"
                );
                runBuilder.directory(new File(reportDir));
                runBuilder.redirectErrorStream(true);

                Process runProcess = runBuilder.start();
                int runExitCode = runProcess.waitFor();

                String runOutput = readProcessOutput(runProcess);

                if (runExitCode == 0) {
                    System.out.println("Java visualization program executed successfully");
                    System.out.println("Generated charts should be in the report directory");
                } else {
                    System.err.println("Error running Java program. Exit code: " + runExitCode);
                    System.err.println("Output: " + runOutput);
                }

            } else {
                System.err.println("Error compiling Java program. Exit code: " + compileExitCode);
                System.err.println("Compilation output: " + compileOutput);
            }

        } catch (Exception e) {
            System.err.println("Error compiling/running Java code: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getClasspath() {
        // Get the current classpath and add JFreeChart libraries
        String currentClasspath = System.getProperty("java.class.path");
        return currentClasspath;
    }
    
    private String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        return output.toString();
    }
    
    public String analyzeVectorData(String vectorData, String analysisPrompt) throws Exception {
        // Create the payload for AI analysis with vector data
        Map<String, Object> payload = createVectorDataAnalysisPayload(vectorData, analysisPrompt);
        
        // Send message to AI API
        String taskId = sendMessageToAI(payload);
        
        // Poll for completion and get result
        String result = pollForCompletion(taskId);
        
        return result;
    }
    
    public String analyzeReportForAnomalies(String pdfPath) throws Exception {
        String anomalyPrompt = """
            Analyze this CloudWatch comprehensive report and identify:
            1. Any unusual spikes or drops in metrics
            2. Patterns that deviate from normal behavior
            3. Potential system anomalies or issues
            4. Correlation between different metrics that might indicate problems
            5. Time periods with suspicious activity
            6. Recommendations for investigation or immediate action
            
            Focus on identifying actionable anomalies that require attention.
            """;
        
        return analyzePDFReport(pdfPath, anomalyPrompt);
    }
    
    public String analyzeReportForCapacityPlanning(String pdfPath) throws Exception {
        String capacityPrompt = """
            Analyze this CloudWatch comprehensive report for capacity planning:
            1. Current resource utilization trends
            2. Growth patterns in metrics over time
            3. Projected resource needs based on current trends
            4. Recommendations for scaling (up/down/out)
            5. Cost optimization opportunities
            6. Timeline for capacity adjustments
            
            Provide specific recommendations with estimated timelines and priorities.
            """;
        
        return analyzePDFReport(pdfPath, capacityPrompt);
    }
    
    private Map<String, Object> createPDFAnalysisPayload(String fileName, String base64PDF, String analysisPrompt) {
        Map<String, Object> payload = new HashMap<>();
        
        payload.put("username", aiUsername);
        payload.put("apikey", aiApiKey);
        payload.put("conv_id", "");
        payload.put("application", application);
        
        // Create messages array with PDF content
        List<Map<String, String>> messages = new ArrayList<>();
        
        // First message with PDF content
        Map<String, String> pdfMessage = new HashMap<>();
        pdfMessage.put("user", "Please analyze this CloudWatch comprehensive report PDF file. The file contains multiple charts and graphs showing CloudWatch metrics over time. I need you to analyze the visual data in the charts, not just the text. The file name is: " + fileName + ". Here is the PDF file content as base64: " + base64PDF);
        messages.add(pdfMessage);
        
        // Second message with analysis request
        Map<String, String> analysisMessage = new HashMap<>();
        analysisMessage.put("user", analysisPrompt);
        messages.add(analysisMessage);
        
        // Third message for output format
        Map<String, String> formatMessage = new HashMap<>();
        formatMessage.put("user", "IMPORTANT: Please analyze the actual charts and graphs in the PDF file. I need you to look at the visual data points, trends, spikes, and patterns in the graphs. Provide specific values, timestamps, and detailed observations from the charts. Do not just refer to chart filenames - analyze the actual visual data.");
        messages.add(formatMessage);
        
        payload.put("messages", messages);
        payload.put("promptfilename", "");
        payload.put("promptname", "");
        payload.put("prompttype", "system");
        payload.put("promptrole", "act as a CloudWatch and system performance expert who can analyze PDF reports with charts and graphs");
        payload.put("prompttask", "");
        payload.put("promptexamples", "");
        payload.put("promptformat", "");
        payload.put("promptrestrictions", "");
        payload.put("promptadditional", "");
        payload.put("max_tokens", maxTokens);
        payload.put("model_type", modelType);
        payload.put("temperature", temperature);
        payload.put("topKChunks", 2);
        payload.put("read_from_your_data", false);
        payload.put("data_filenames", new ArrayList<>());
        payload.put("document_groupname", "");
        payload.put("document_grouptags", new ArrayList<>());
        payload.put("find_the_best_response", false);
        payload.put("chat_attr", new HashMap<>());
        payload.put("additional_attr", new HashMap<>());
        
        return payload;
    }
    
    private Map<String, Object> createTextAnalysisPayload(String fileName, String pdfTextContent, String analysisPrompt) {
        Map<String, Object> payload = new HashMap<>();
        
        payload.put("username", aiUsername);
        payload.put("apikey", aiApiKey);
        payload.put("conv_id", "");
        payload.put("application", application);
        
        // Create messages array with text content
        List<Map<String, String>> messages = new ArrayList<>();
        
        // First message with PDF text content
        Map<String, String> pdfMessage = new HashMap<>();
        pdfMessage.put("user", "Here is the text content extracted from a CloudWatch comprehensive report PDF file (" + fileName + "). NOTE: This is only the text content - the visual chart data could not be extracted. Please analyze based on available text and clearly indicate that detailed chart analysis requires access to the visual graphs:\n\n" + pdfTextContent);
        messages.add(pdfMessage);
        
        // Second message with analysis request
        Map<String, String> analysisMessage = new HashMap<>();
        analysisMessage.put("user", analysisPrompt);
        messages.add(analysisMessage);
        
        // Third message for output format
        Map<String, String> formatMessage = new HashMap<>();
        formatMessage.put("user", "Please provide analysis based on the available text content. For any chart references, clearly state that detailed visual analysis of the charts would require access to the actual graph images and recommend reviewing the specific chart files mentioned.");
        messages.add(formatMessage);
        
        payload.put("messages", messages);
        payload.put("promptfilename", "");
        payload.put("promptname", "");
        payload.put("prompttype", "system");
        payload.put("promptrole", "act as a CloudWatch and system performance expert");
        payload.put("prompttask", "");
        payload.put("promptexamples", "");
        payload.put("promptformat", "");
        payload.put("promptrestrictions", "");
        payload.put("promptadditional", "");
        payload.put("max_tokens", maxTokens);
        payload.put("model_type", modelType);
        payload.put("temperature", temperature);
        payload.put("topKChunks", 2);
        payload.put("read_from_your_data", false);
        payload.put("data_filenames", new ArrayList<>());
        payload.put("document_groupname", "");
        payload.put("document_grouptags", new ArrayList<>());
        payload.put("find_the_best_response", false);
        payload.put("chat_attr", new HashMap<>());
        payload.put("additional_attr", new HashMap<>());
        
        return payload;
    }
    
    private Map<String, Object> createVectorDataAnalysisPayload(String vectorData, String analysisPrompt) {
        Map<String, Object> payload = new HashMap<>();
        
        payload.put("username", aiUsername);
        payload.put("apikey", aiApiKey);
        payload.put("conv_id", "");
        payload.put("application", application);
        
        // Create messages array with vector data content
        List<Map<String, String>> messages = new ArrayList<>();
        
        // First message with vector data content
        Map<String, String> dataMessage = new HashMap<>();
        dataMessage.put("user", "Here is CloudWatch metrics vector data in structured JSON format. This contains raw numerical data with timestamps, averages, minimums, and maximums for various metrics:\n\n" + vectorData);
        messages.add(dataMessage);
        
        // Second message with analysis request
        Map<String, String> analysisMessage = new HashMap<>();
        analysisMessage.put("user", analysisPrompt);
        messages.add(analysisMessage);
        
        // Third message for output format
        Map<String, String> formatMessage = new HashMap<>();
        formatMessage.put("user", "Please provide detailed numerical analysis based on the vector data. Include specific values, timestamps, statistical calculations, and data-driven insights. Use the exact timestamps and values from the data points.");
        messages.add(formatMessage);
        
        payload.put("messages", messages);
        payload.put("promptfilename", "");
        payload.put("promptname", "");
        payload.put("prompttype", "system");
        payload.put("promptrole", "act as a CloudWatch metrics and data analysis expert who can perform detailed numerical analysis on time-series data");
        payload.put("prompttask", "");
        payload.put("promptexamples", "");
        payload.put("promptformat", "");
        payload.put("promptrestrictions", "");
        payload.put("promptadditional", "");
        payload.put("max_tokens", maxTokens);
        payload.put("model_type", modelType);
        payload.put("temperature", temperature);
        payload.put("topKChunks", 2);
        payload.put("read_from_your_data", false);
        payload.put("data_filenames", new ArrayList<>());
        payload.put("document_groupname", "");
        payload.put("document_grouptags", new ArrayList<>());
        payload.put("find_the_best_response", false);
        payload.put("chat_attr", new HashMap<>());
        payload.put("additional_attr", new HashMap<>());
        
        return payload;
    }
    
    private String sendMessageToAI(Map<String, Object> payload) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("accept", "application/json");
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
            sendMessageUrl,
            HttpMethod.POST,
            request,
            String.class
        );
        
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("AI API call failed with status: " + response.getStatusCode());
        }
        
        // Parse response to get task_id
        JsonNode responseNode = objectMapper.readTree(response.getBody());
        String taskId = responseNode.get("task_id").asText();

        if (taskId == null || taskId.isEmpty()) {
            throw new RuntimeException("Failed to get task_id from AI API response");
        }
        
        return taskId;
    }
    
    private String pollForCompletion(String taskId) throws Exception {
        String status = "";
        String result = "";
        int maxAttempts = 60; // 5 minutes maximum wait time
        int attempt = 0;
        
        while (attempt < maxAttempts) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("accept", "application/json");
            
            HttpEntity<String> request = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                statusUrl + taskId,
                HttpMethod.GET,
                request,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode statusResponse = objectMapper.readTree(response.getBody());
                status = statusResponse.get("status").asText();
                
                System.out.println("AI Analysis Status for task " + taskId + ": " + status);
                
                if ("Complete".equals(status)) {
                    // Extract the result from the response
                    JsonNode resultNode = statusResponse.get("result");
                    if (resultNode != null) {
                        result = resultNode.asText();
                    } else {
                        result = "Analysis completed but no result content found.";
                    }
                    break;
                } else if ("Failed".equals(status)) {
                    throw new RuntimeException("AI analysis failed for task: " + taskId);
                }
            } else {
                throw new RuntimeException("Status API call failed with status: " + response.getStatusCode());
            }
            
            // Wait 5 seconds before next poll
            Thread.sleep(5000);
            attempt++;
        }
        
        if (attempt >= maxAttempts) {
            throw new RuntimeException("AI analysis timed out for task: " + taskId);
        }
        
        return result;
    }
}
