# Stability Analysis with Vector Data

## Overview

The stability analysis endpoint has been updated to use **vector data** (raw numerical metrics) instead of PDF files for AI analysis. This provides more precise analysis with exact values and timestamps.

## Updated Endpoint

### POST /analysis/stability-analysis

**Parameters:**
- `clusterName` (optional): Kafka cluster name
- `dbInstanceIdentifier` (optional): RDS database instance identifier
- `startTime` (required): Start time in ISO 8601 format
- `endTime` (required): End time in ISO 8601 format

**Example Usage:**
```bash
curl -X POST "http://localhost:8080/analysis/stability-analysis" \
  -d "clusterName=kafka" \
  -d "dbInstanceIdentifier=prod-database-1" \
  -d "startTime=2025-09-10T10:00:00.000Z" \
  -d "endTime=2025-09-10T14:00:00.000Z"
```

## How It Works Now

1. **Vector Data Generation**: Creates structured JSON with raw metric data
2. **AI Analysis**: Sends numerical data to AI instead of visual charts
3. **Precise Analysis**: AI can analyze exact values, timestamps, and statistics
4. **Fallback**: If vector data is unavailable, falls back to PDF analysis

## Enhanced Analysis Capabilities

### With Vector Data, AI Can Now:

**DB CPU Utilization Analysis:**
- Calculate exact percentage of time outside 40%-50% range
- Identify specific timestamps of spikes above 50%
- Analyze min/max/average patterns over time

**DB Connections Analysis:**
- Detect exact connection counts outside 2,000-2,500 range
- Report precise timestamps of connection spikes
- Identify peak usage periods with exact values

**Kafka Consumer Lag Analysis:**
- Analyze sum_offset_lag and max_offset_lag with precise numbers
- Compare lag patterns between consumer groups numerically
- Report exact lag values and timestamps

**Memory Usage Analysis:**
- Track freeable_memory trends with byte-level precision
- Calculate memory pressure using exact values
- Identify memory leak patterns numerically

**Statistical Analysis:**
- Calculate stability scores based on time within expected ranges
- Perform correlation analysis between metrics
- Generate data-driven recommendations with specific thresholds

## Sample Output

```
Report Directory: comprehensive_report_2025-09-11T10-46-37-332201900Z
Vector Data File: comprehensive_report_2025-09-11T10-46-37-332201900Z/metrics_vector_data.json

=== STABILITY ANALYSIS (Based on Vector Data) ===

**DB CPU Utilization Analysis:**
- At 2025-09-10T10:10:00Z: CPU spiked to 51.20% average (55.80% maximum) - ABOVE expected range
- At 2025-09-10T10:15:00Z: CPU normalized to 46.30% average - WITHIN expected range
- Time outside expected range: 25% of monitoring period

**DB Connections Analysis:**  
- At 2025-09-10T10:10:00Z: Connections reached 2,456 average (2,489 maximum) - WITHIN expected range
- Peak connection period: 10:05-10:15 UTC with sustained high usage
- No violations of 2,000-2,500 connection limit detected

**Kafka Consumer Lag Analysis:**
- Worker group: Highest lag at 2025-09-10T10:10:00Z with 156.75 average lag
- Notify group: Stable lag patterns, maximum 52.10 average lag
- Async_notify group: Lowest lag, maximum 27.60 average lag

**Overall Stability Assessment:**
- System showed 75% stability during monitoring period
- CPU spike correlated with high connection usage at 10:10 UTC
- Recommendation: Monitor CPU usage patterns and consider scaling if sustained above 50%
```

## Benefits Over PDF Analysis

1. **Exact Values**: AI gets precise numbers instead of visual approximations
2. **Statistical Analysis**: Can calculate percentages, averages, correlations
3. **Timestamp Precision**: Exact timing of issues, not visual estimates
4. **Automated Calculations**: AI can compute stability scores and metrics
5. **Data-Driven Insights**: Recommendations based on numerical thresholds
6. **Faster Processing**: JSON processing vs image/PDF parsing

## Implementation Details

- **Vector Data File**: `metrics_vector_data.json` contains structured metrics
- **New AI Method**: `analyzeVectorDataForStability()` handles numerical analysis
- **Enhanced Prompts**: Specialized prompts for statistical analysis
- **Fallback Support**: Still supports PDF analysis if vector data fails
- **Error Handling**: Graceful degradation to PDF if AWS credentials expired
