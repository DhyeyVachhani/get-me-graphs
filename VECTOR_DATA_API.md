# CloudWatch Vector Data Extraction for AI Analysis

## Overview

This solution provides a way to extract raw metric data (vector data) from CloudWatch for analysis with LLMs like ChatGPT, which cannot process visual charts but can analyze numerical data.

## Problem Solved

**Before**: AI/LLMs could not analyze CloudWatch charts because they are visual/image files.
**After**: AI/LLMs can now analyze raw metric data in structured JSON format with timestamps, averages, minimums, and maximums.

## New API Endpoint

### Get Metrics Vector Data
```
GET /analysis/metrics-vector-data
```

**Parameters:**
- `clusterName` (optional): Kafka cluster name (e.g., "kafka")
- `dbInstanceIdentifier` (optional): RDS database instance identifier (e.g., "prod-database-1")
- `startTime` (required): Start time in ISO 8601 format (e.g., "2025-09-10T10:00:00.000Z")
- `endTime` (required): End time in ISO 8601 format (e.g., "2025-09-10T14:00:00.000Z")

**Example Usage:**
```bash
curl "http://localhost:8080/analysis/metrics-vector-data?clusterName=kafka&dbInstanceIdentifier=prod-database-1&startTime=2025-09-10T10:00:00.000Z&endTime=2025-09-10T14:00:00.000Z"
```

## Response Format

The endpoint returns structured JSON containing:

### Metadata
- Generation timestamp
- Time range queried

### RDS Metrics (if dbInstanceIdentifier provided)
- **CPU Utilization**: Percentage with expected range (40% - 50%)
- **Database Connections**: Connection count with expected range (2000 - 2500)
- **Freeable Memory**: Available memory in bytes

### Kafka Metrics (if clusterName provided)
- **Consumer Groups**: worker, notify, async_notify
- **Metrics per group**:
  - **Sum Offset Lag**: Total messages behind
  - **Max Offset Lag**: Maximum lag across partitions

### Data Point Structure
Each metric includes:
```json
{
  "timestamp": "2025-09-10T10:00:00Z",
  "average": 42.50,
  "maximum": 45.20,
  "minimum": 38.10
}
```

## How to Use with ChatGPT

1. **Call the API endpoint** to get vector data
2. **Copy the JSON response**
3. **Paste into ChatGPT** with a prompt like:

```
Please analyze this CloudWatch metrics data for performance issues, anomalies, or capacity concerns:

[PASTE JSON DATA HERE]

Focus on:
- CPU utilization patterns and spikes
- Database connection trends
- Kafka consumer lag issues
- Memory usage patterns
- Any unusual patterns or anomalies
```

## Sample Data Structure

See `sample_metrics_vector_data.json` for a complete example of the response format.

## Benefits

1. **AI-Readable Format**: LLMs can analyze structured numerical data
2. **Complete Context**: Includes timestamps, averages, min/max values
3. **Expected Ranges**: Shows what values are considered normal
4. **Multiple Metrics**: Combines RDS and Kafka metrics in one response
5. **Time-Series Data**: Shows trends over time periods

## File Generation

When called, the endpoint:
1. Generates a comprehensive report with charts (for human viewing)
2. Creates a `metrics_vector_data.json` file (for AI analysis)
3. Returns the JSON content directly via HTTP response

## Error Handling

- If AWS credentials are invalid/expired, returns error message
- If no data is found, returns empty data points array
- If metric retrieval fails, logs error and continues with other metrics
