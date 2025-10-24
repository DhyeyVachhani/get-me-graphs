# Comprehensive PDF Report API

## Overview
The comprehensive PDF report API generates a single PDF file containing:
1. Report summary (from report_summary.txt)
2. All generated charts with proper headings and titles
3. Organized sections for RDS and Kafka metrics

## API Endpoints

### 1. Comprehensive PDF Report
```
POST /cloudwatch/export/comprehensive-pdf-report
```

### 2. Advanced Kafka Lag Metrics (NEW)
```
POST /cloudwatch/export/kafka-advanced-lag
```

### 3. Multi-Metric Kafka Lag Chart (NEW)
```
POST /cloudwatch/export/kafka-multi-metric-lag
```

### 4. Dashboard Kafka Lag Chart (NEW)
```
POST /cloudwatch/export/kafka-dashboard-lag
```

## Parameters

### Comprehensive PDF Report
- `clusterName` (optional) - Kafka cluster name for Kafka metrics
- `dbInstanceIdentifier` (optional) - RDS database instance identifier for RDS metrics
- `startTime` (required) - Start time in ISO 8601 format (e.g., "2024-06-01T00:00:00Z")
- `endTime` (required) - End time in ISO 8601 format (e.g., "2024-06-01T23:59:59Z")

### Advanced Kafka Lag Metrics
- `clusterName` (required) - Kafka cluster name
- `consumerGroup` (optional) - Specific consumer group (if not provided, shows all groups)
- `topic` (optional) - Specific topic (if not provided with consumerGroup, auto-discovers topics)
- `metricName` (optional, default: "SumOffsetLag") - Lag metric to use:
  - `SumOffsetLag` - Sum of offset lag across partitions
  - `MaxOffsetLag` - Maximum offset lag across partitions
  - `RollingEstimatedTimeLagMax` - Rolling estimated maximum time lag
  - `EstimatedMaxTimeLag` - Estimated maximum time lag
- `startTime` (required) - Start time in ISO 8601 format
- `endTime` (required) - End time in ISO 8601 format

### Multi-Metric Kafka Lag Chart
- `clusterName` (required) - Kafka cluster name
- `consumerGroup` (required) - Consumer group name
- `topic` (required) - Topic name
- `startTime` (required) - Start time in ISO 8601 format
- `endTime` (required) - End time in ISO 8601 format

### Dashboard Kafka Lag Chart
- `clusterName` (required) - Kafka cluster name (e.g., "your-kafka-cluster")
- `startTime` (required) - Start time in ISO 8601 format
- `endTime` (required) - End time in ISO 8601 format

## Example Usage

### Advanced Kafka Lag - Specific Consumer Group and Topic with SumOffsetLag
```bash
curl -X POST "http://localhost:8080/cloudwatch/export/kafka-advanced-lag" \
  -d "clusterName=your-kafka-cluster" \
  -d "consumerGroup=your-consumer-group" \
  -d "topic=your-topic" \
  -d "metricName=SumOffsetLag" \
  -d "startTime=2024-06-01T00:00:00Z" \
  -d "endTime=2024-06-01T23:59:59Z"
```

### Advanced Kafka Lag - All Topics for Specific Consumer Group with MaxOffsetLag
```bash
curl -X POST "http://localhost:8080/cloudwatch/export/kafka-advanced-lag" \
  -d "clusterName=your-kafka-cluster" \
  -d "consumerGroup=your-consumer-group" \
  -d "metricName=MaxOffsetLag" \
  -d "startTime=2024-06-01T00:00:00Z" \
  -d "endTime=2024-06-01T23:59:59Z"
```

### Advanced Kafka Lag - All Consumer Groups with EstimatedMaxTimeLag
```bash
curl -X POST "http://localhost:8080/cloudwatch/export/kafka-advanced-lag" \
  -d "clusterName=your-kafka-cluster" \
  -d "metricName=EstimatedMaxTimeLag" \
  -d "startTime=2024-06-01T00:00:00Z" \
  -d "endTime=2024-06-01T23:59:59Z"
```

### Multi-Metric Lag Chart - All Metrics in One Chart
```bash
curl -X POST "http://localhost:8080/cloudwatch/export/kafka-multi-metric-lag" \
  -d "clusterName=your-kafka-cluster" \
  -d "consumerGroup=your-consumer-group" \
  -d "topic=your-topic" \
  -d "startTime=2024-06-01T00:00:00Z" \
  -d "endTime=2024-06-01T23:59:59Z"
```

### Dashboard Lag Chart - Example
```bash
curl -X POST "http://localhost:8080/cloudwatch/export/kafka-dashboard-lag" \
  -d "clusterName=your-kafka-cluster" \
  -d "startTime=2024-06-01T00:00:00Z" \
  -d "endTime=2024-06-01T23:59:59Z"
```

### For Both RDS and Kafka Metrics (PDF)
```bash
curl -X POST "http://localhost:8080/cloudwatch/export/comprehensive-pdf-report" \
  -d "clusterName=your-kafka-cluster" \
  -d "dbInstanceIdentifier=your-rds-instance" \
  -d "startTime=2024-06-01T00:00:00Z" \
  -d "endTime=2024-06-01T23:59:59Z"
```

## Supported Kafka Lag Metrics

1. **SumOffsetLag** - Sum of offset lag across all partitions for a consumer group/topic
2. **MaxOffsetLag** - Maximum offset lag across all partitions for a consumer group/topic
3. **RollingEstimatedTimeLagMax** - Rolling window of estimated maximum time lag
4. **EstimatedMaxTimeLag** - Estimated maximum time lag

## Dashboard Configuration (Sanitized)

For demonstration, you can pre-configure up to three representative consumer groups and topics (replace with your own values):

### Example Placeholder Consumer Groups and Topics:
1. **Worker Consumer**:
   - Consumer Group: `your-consumer-group-worker`
   - Topic: `your-topic-worker`

2. **Notify Consumer**:
   - Consumer Group: `your-consumer-group-notify`
   - Topic: `your-topic-notify`

3. **Async Notify Consumer**:
   - Consumer Group: `your-consumer-group-async-notify`
   - Topic: `your-topic-async-notify`

### Metrics for Each Consumer Group:
- SumOffsetLag
- MaxOffsetLag
- RollingEstimatedTimeLagMax
- EstimatedMaxTimeLag

### Chart Features:
- **12 Series Total**: 3 consumer groups × 4 metrics each
- **Simplified Legend**: Consumer groups shortened for readability
- **Mixed Units**: Handles both message counts and time lag metrics
- **Error Resilience**: Continues if some series fail to load

## API Features

### Advanced Kafka Lag API
- **Flexible Metric Selection**: Choose from 4 different lag metrics
- **Multiple Scope Options**: 
  - Specific consumer group + topic
  - All topics for a consumer group (auto-discovery)
  - All consumer groups in cluster
- **Proper Y-Axis Labels**: Automatically sets appropriate units (Messages vs Milliseconds)

### Multi-Metric Lag Chart API
- **Single Chart View**: Shows all 4 lag metrics on one chart for comparison
- **Mixed Units Warning**: Y-axis labeled as "Mixed Units" since metrics have different scales
- **Complete Overview**: See all lag perspectives at once

### Dashboard API
- **Configurable Replication**: Mirrors any internal dashboard configuration using placeholders
- **Static Configuration Option**: Pre-configured consumer groups and topics (no parameters needed except clusterName & time window)
- **Comprehensive View**: Shows multiple lag metrics in one chart
- **Readable Legend**: Simplified consumer group names for better readability

### Error Handling
- **Individual Metric Failures**: If one metric fails, others still generate
- **Robust Discovery**: Auto-discovery works with any available metrics
- **Clear Error Messages**: Detailed error reporting for troubleshooting

## Output Files

- Advanced Lag: `kafka_advanced_lag_{metricname}.png`
- Multi-Metric: `kafka_multi_metric_lag_{consumergroup}_{topic}.png`
- Dashboard: `kafka_dashboard_lag_{clustername}.png`
- PDF Report: `comprehensive_report_{timestamp}.pdf`

## Notes

- All APIs maintain UTC timezone for X-axis
- Charts are automatically scaled and formatted
- The multi-metric chart shows all metrics but uses mixed units (be aware when interpreting)
- This document uses placeholders instead of real internal identifiers for security reasons
- Replace `your-kafka-cluster`, `your-consumer-group-*`, and `your-topic-*` with your own values when invoking endpoints
