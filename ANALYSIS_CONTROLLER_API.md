# Analysis Controller API Documentation

## Overview
The `AnalysisController` is a dedicated controller for all AI-powered analysis operations. It has been separated from the `CloudWatchChartController` to provide better organization and cleaner separation of concerns.

## Base URL
All analysis endpoints are now available under:
```
/analysis
```

## Available Endpoints

### 1. Comprehensive Report Analysis
**Endpoint:** `POST /analysis/comprehensive-report`

**Description:** Generates a comprehensive PDF report with charts and provides AI analysis.

**Parameters:**
- `clusterName` (optional) - Kafka cluster name for Kafka metrics
- `dbInstanceIdentifier` (optional) - RDS instance identifier for database metrics  
- `startTime` (required) - Start time in ISO 8601 format (e.g., 2024-06-01T00:00:00Z)
- `endTime` (required) - End time in ISO 8601 format (e.g., 2024-06-01T23:59:59Z)
- `analysisType` (optional) - Type of analysis: "performance", "anomalies", "capacity", or "custom"
- `customPrompt` (optional) - Custom analysis prompt (required if analysisType is "custom")

**Example:**
```bash
curl -X POST "http://localhost:8080/analysis/comprehensive-report" \
  -d "clusterName=my-kafka-cluster&dbInstanceIdentifier=my-rds-instance&startTime=2024-09-01T00:00:00Z&endTime=2024-09-01T23:59:59Z&analysisType=anomalies"
```

### 2. Existing Report Analysis
**Endpoint:** `POST /analysis/existing-report`

**Description:** Analyzes an existing PDF report without generating new charts.

**Parameters:**
- `pdfPath` (required) - Path to the existing PDF report
- `analysisType` (optional) - Type of analysis: "performance", "anomalies", "capacity", or "custom"
- `customPrompt` (optional) - Custom analysis prompt (required if analysisType is "custom")

**Example:**
```bash
curl -X POST "http://localhost:8080/analysis/existing-report" \
  -d "pdfPath=/path/to/report.pdf&analysisType=performance"
```

### 3. Performance Insights Analysis
**Endpoint:** `POST /analysis/performance-insights`

**Description:** Generates a comprehensive report and provides performance-focused analysis.

**Parameters:**
- `clusterName` (optional) - Kafka cluster name
- `dbInstanceIdentifier` (optional) - RDS instance identifier
- `startTime` (required) - Start time in ISO 8601 format
- `endTime` (required) - End time in ISO 8601 format

**Example:**
```bash
curl -X POST "http://localhost:8080/analysis/performance-insights" \
  -d "clusterName=my-kafka-cluster&startTime=2024-09-01T00:00:00Z&endTime=2024-09-01T23:59:59Z"
```

### 4. Anomaly Detection Analysis
**Endpoint:** `POST /analysis/anomaly-detection`

**Description:** Generates a comprehensive report and identifies anomalies and unusual patterns.

**Parameters:**
- `clusterName` (optional) - Kafka cluster name
- `dbInstanceIdentifier` (optional) - RDS instance identifier
- `startTime` (required) - Start time in ISO 8601 format
- `endTime` (required) - End time in ISO 8601 format

**Example:**
```bash
curl -X POST "http://localhost:8080/analysis/anomaly-detection" \
  -d "dbInstanceIdentifier=my-rds-instance&startTime=2024-09-01T00:00:00Z&endTime=2024-09-01T23:59:59Z"
```

### 5. Capacity Planning Analysis
**Endpoint:** `POST /analysis/capacity-planning`

**Description:** Generates a comprehensive report and provides capacity planning insights.

**Parameters:**
- `clusterName` (optional) - Kafka cluster name
- `dbInstanceIdentifier` (optional) - RDS instance identifier
- `startTime` (required) - Start time in ISO 8601 format
- `endTime` (required) - End time in ISO 8601 format

**Example:**
```bash
curl -X POST "http://localhost:8080/analysis/capacity-planning" \
  -d "clusterName=my-kafka-cluster&dbInstanceIdentifier=my-rds-instance&startTime=2024-09-01T00:00:00Z&endTime=2024-09-01T23:59:59Z"
```

### 6. Custom Analysis
**Endpoint:** `POST /analysis/custom-analysis`

**Description:** Generates a comprehensive report and provides custom analysis based on your specific prompt.

**Parameters:**
- `clusterName` (optional) - Kafka cluster name
- `dbInstanceIdentifier` (optional) - RDS instance identifier
- `startTime` (required) - Start time in ISO 8601 format
- `endTime` (required) - End time in ISO 8601 format
- `customPrompt` (required) - Your custom analysis prompt

**Example:**
```bash
curl -X POST "http://localhost:8080/analysis/custom-analysis" \
  -d "clusterName=my-kafka-cluster&startTime=2024-09-01T00:00:00Z&endTime=2024-09-01T23:59:59Z&customPrompt=Analyze the correlation between Kafka lag and database performance"
```

## Migration from Old Endpoints

The old endpoints under `/cloudwatch/analyze/*` have been moved to `/analysis/*`:

| Old Endpoint | New Endpoint |
|--------------|--------------|
| `/cloudwatch/analyze/comprehensive-report` | `/analysis/comprehensive-report` |
| `/cloudwatch/analyze/existing-report` | `/analysis/existing-report` |
| `/cloudwatch/analyze/performance-insights` | `/analysis/performance-insights` |
| `/cloudwatch/analyze/anomaly-detection` | `/analysis/anomaly-detection` |
| `/cloudwatch/analyze/capacity-planning` | `/analysis/capacity-planning` |

## Response Format

All analysis endpoints return a string response in the following format:

```
PDF Report: [path-to-generated-pdf]

=== [ANALYSIS TYPE] ===
[AI Analysis Results]
```

## Error Handling

If an error occurs, the endpoints return an error message in the format:
```
Error: [error message]
```

## Dependencies

The `AnalysisController` depends on:
- `CloudWatchChartService` - For generating PDF reports
- `AIAnalysisService` - For AI-powered analysis

## Security Note

The controller uses the SSL configuration from `RestTemplateConfig` to handle corporate certificate requirements when communicating with the AI analysis service.
