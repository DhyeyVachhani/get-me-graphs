# AI Analysis API Documentation

This document describes the new AI analysis endpoints that generate comprehensive CloudWatch reports and analyze them using AI.

## Overview

The AI Analysis feature generates comprehensive PDF reports from CloudWatch metrics and then uses AI to analyze the reports for various insights like performance bottlenecks, anomalies, and capacity planning recommendations.

## Endpoints

### 1. Generate and Analyze Comprehensive Report
**POST** `/cloudwatch/analyze/comprehensive-report`

Generates a comprehensive PDF report and analyzes it with AI.

**Parameters:**
- `clusterName` (optional): Kafka cluster name for Kafka metrics
- `dbInstanceIdentifier` (optional): RDS database instance identifier for RDS metrics  
- `startTime` (required): Start time in ISO 8601 format (e.g., "2024-06-01T00:00:00Z")
- `endTime` (required): End time in ISO 8601 format (e.g., "2024-06-01T23:59:59Z")
- `analysisType` (optional): Type of analysis - "performance", "anomalies", "capacity", or "custom" (default: "performance")
- `customPrompt` (optional): Custom analysis prompt (required if analysisType is "custom")

**Example:**
```bash
curl -X POST "http://localhost:8080/cloudwatch/analyze/comprehensive-report" \
  -d "clusterName=your-kafka-cluster" \
  -d "dbInstanceIdentifier=your-rds-instance" \
  -d "startTime=2024-06-01T00:00:00Z" \
  -d "endTime=2024-06-01T23:59:59Z" \
  -d "analysisType=performance"
```

### 2. Analyze Existing Report
**POST** `/cloudwatch/analyze/existing-report`

Analyzes an existing PDF report.

**Parameters:**
- `pdfPath` (required): Path to the existing PDF report
- `analysisType` (optional): Type of analysis - "performance", "anomalies", "capacity", or "custom" (default: "performance")
- `customPrompt` (optional): Custom analysis prompt (required if analysisType is "custom")

**Example:**
```bash
curl -X POST "http://localhost:8080/cloudwatch/analyze/existing-report" \
  -d "pdfPath=C:/reports/comprehensive_report_2024-06-01.pdf" \
  -d "analysisType=anomalies"
```

### 3. Performance Insights Analysis
**POST** `/cloudwatch/analyze/performance-insights`

Generates a report and analyzes it specifically for performance insights.

**Parameters:**
- `clusterName` (optional): Kafka cluster name
- `dbInstanceIdentifier` (optional): RDS database instance identifier
- `startTime` (required): Start time in ISO 8601 format
- `endTime` (required): End time in ISO 8601 format

**Example:**
```bash
curl -X POST "http://localhost:8080/cloudwatch/analyze/performance-insights" \
  -d "clusterName=your-kafka-cluster" \
  -d "dbInstanceIdentifier=your-rds-instance" \
  -d "startTime=2024-06-01T00:00:00Z" \
  -d "endTime=2024-06-01T23:59:59Z"
```

### 4. Anomaly Detection Analysis
**POST** `/cloudwatch/analyze/anomaly-detection`

Generates a report and analyzes it specifically for anomalies and unusual patterns.

**Parameters:**
- `clusterName` (optional): Kafka cluster name
- `dbInstanceIdentifier` (optional): RDS database instance identifier
- `startTime` (required): Start time in ISO 8601 format
- `endTime` (required): End time in ISO 8601 format

**Example:**
```bash
curl -X POST "http://localhost:8080/cloudwatch/analyze/anomaly-detection" \
  -d "clusterName=your-kafka-cluster" \
  -d "startTime=2024-06-01T00:00:00Z" \
  -d "endTime=2024-06-01T23:59:59Z"
```

### 5. Capacity Planning Analysis
**POST** `/cloudwatch/analyze/capacity-planning`

Generates a report and analyzes it specifically for capacity planning and resource optimization.

**Parameters:**
- `clusterName` (optional): Kafka cluster name
- `dbInstanceIdentifier` (optional): RDS database instance identifier
- `startTime` (required): Start time in ISO 8601 format
- `endTime` (required): End time in ISO 8601 format

**Example:**
```bash
curl -X POST "http://localhost:8080/cloudwatch/analyze/capacity-planning" \
  -d "dbInstanceIdentifier=your-rds-instance" \
  -d "startTime=2024-06-01T00:00:00Z" \
  -d "endTime=2024-06-01T23:59:59Z"
```

## Analysis Types

### Performance Analysis
- Identifies performance bottlenecks
- Analyzes resource utilization patterns
- Provides optimization recommendations
- Reviews Kafka consumer lag issues
- Assesses database performance metrics

### Anomaly Detection
- Identifies unusual spikes or drops in metrics
- Detects patterns that deviate from normal behavior
- Finds correlations between metrics that indicate problems
- Highlights suspicious time periods
- Provides investigation recommendations

### Capacity Planning
- Analyzes current resource utilization trends
- Identifies growth patterns in metrics
- Projects future resource needs
- Recommends scaling strategies
- Suggests cost optimization opportunities

### Custom Analysis
- Allows you to provide your own analysis prompt
- Flexible analysis based on your specific requirements
- Can combine multiple analysis types
- Tailored insights for specific use cases

## Response Format

All endpoints return a text response containing:
1. Path to the generated PDF report (if applicable)
2. AI analysis results with structured insights
3. Actionable recommendations

## Error Handling

- Invalid parameters will return error messages
- AI API failures will be reported with specific error details
- File not found errors for existing report analysis
- Timeout errors if AI analysis takes too long (5-minute timeout)

## Configuration

The AI analysis service is configured through `application.properties` with the following properties:

```properties
# AI Analysis Configuration (sanitize for public repo)
ai.analysis.username=your-username
aio.analysis.apikey=your-api-key
ai.analysis.send-message-url=https://your-ai-service.example/api/v1/chats/send-message
ai.analysis.status-url=https://your-ai-service.example/api/v1/chats/status/
ai.analysis.application=cloudwatch-analysis
ai.analysis.max-tokens=4000
ai.analysis.model-type=GPT4o_128K
ai.analysis.temperature=0.1
```

### Configuration Properties Explained:

- **`ai.analysis.username`**: Your username for the AI API
- **`ai.analysis.apikey`**: Your API key for authentication
- **`ai.analysis.send-message-url`**: The endpoint URL for sending messages to AI (e.g., https://your-ai-service.example/api/v1/chats/send-message)
- **`ai.analysis.status-url`**: The endpoint URL for checking analysis status (e.g., https://your-ai-service.example/api/v1/chats/status/)
- **`ai.analysis.application`**: Application identifier for the AI service
- **`ai.analysis.max-tokens`**: Maximum tokens for AI response (default: 4000)
- **`ai.analysis.model-type`**: AI model to use (default: GPT4o_128K)
- **`ai.analysis.temperature`**: Response creativity level (0.1 for factual, consistent responses)

## Notes

- PDF reports are generated with professional charts including average values
- AI analysis includes charts, metrics, and metadata from the PDF
- The system polls for AI completion every 5 seconds
- Large PDFs may take longer to analyze
- Ensure proper AWS credentials are configured for CloudWatch access
