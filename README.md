# Get Me Graphs

Get Me Graphs is a Spring Boot (Java 17) application that automates:
1. Retrieval of AWS CloudWatch metrics (RDS & Kafka).
2. Generation of high-quality time‑series charts (PNG) using JFreeChart.
3. Creation of comprehensive PDF reports (iText + embedded charts).
4. Export of raw "vector" JSON metric data for LLM-friendly numeric analysis.
5. AI-driven analysis of reports/data (performance, stability, anomalies, capacity, custom prompts).
6. Automatic extraction, compilation, and execution of AI‑generated Java visualization code.

---
## Why This Project
Recruiters and engineering teams can quickly evaluate experience across AWS SDK, time-series visualization, PDF composition, AI workflow integration, and runtime code generation/automation.

---
## Key Features
- AWS CloudWatch integration (Kafka consumer lag, Kafka broker CPU, RDS CPU, memory, DB connections, IOPS).
- Auto-discovery of Kafka topics, consumer groups, and brokers.
- Enhanced chart styling (modern palette, average subtitles, timezone-aware axes).
- Comprehensive report directory with PNG charts + metrics JSON + optional PDF.
- Raw vector metrics JSON for direct numeric / statistical AI analysis.
- AI analysis service (task submission + polling) with multiple prompt profiles.
- Stability analysis prompt can yield executable Java charting program (auto-injected metrics data and compiled).
- Fallback PDF text extraction (Apache PDFBox) if binary upload fails.

---
## Architecture Overview
**Controllers**
- `CloudWatchChartController`: Endpoints for exporting charts & comprehensive reports.
- `AnalysisController`: Endpoints for AI-driven analyses (performance, stability, custom, anomalies, capacity).

**Services**
- `CloudWatchChartService`: Metric retrieval + JFreeChart generation + PDF assembly.
- `AIAnalysisService`: AI API integration, polling, Java code extraction & dynamic compilation.

**Config**
- `RestTemplateConfig`: Custom `RestTemplate` with optional trust-all SSL (dev use only).

**Entry Point**
- `GetMeGraphsApplication`: Spring Boot main application.

---
## Tech Stack
- Java 17
- Spring Boot 3.5.x
- AWS SDK v2 (CloudWatch)
- JFreeChart & JCommon
- iText (PDF)
- Apache PDFBox (text extraction)
- External AI REST API
- Maven build lifecycle

---
## Getting Started
### Prerequisites
- Java 17
- Maven 3.9+
- Valid AWS credentials (AccessKey, SecretKey, SessionToken) with CloudWatch read permissions
- AI API credentials (username, apikey)
- Network access to the AI endpoints configured in properties

### Clone & Configure
```bash
git clone https://github.com/YOUR_GITHUB_USERNAME/get-me-graphs.git
cd get-me-graphs
copy application.properties.sample src/main/resources/application.properties  # (Windows)
# or
cp application.properties.sample src/main/resources/application.properties    # (Linux/macOS)
```
Edit `src/main/resources/application.properties` with real AWS & AI values.

### Build
```bash
mvn clean compile
```

### Run
```bash
mvn spring-boot:run
```
Application starts on `http://localhost:8080` by default.

---
## Core Endpoints (Summary)
| Endpoint | Method | Description |
|----------|--------|-------------|
| /cloudwatch/export | POST | Export basic RDS CPU & memory charts |
| /cloudwatch/export/database-connections | POST | RDS DB connections chart |
| /cloudwatch/export/database-read-iops | POST | RDS Read IOPS chart |
| /cloudwatch/export/database-write-iops | POST | RDS Write IOPS chart |
| /cloudwatch/export/kafka-consumer-lag | POST | Kafka consumer lag (single group/topic) |
| /cloudwatch/export/kafka-cpu-usage-by-broker | POST | Kafka broker system CPU usage |
| /cloudwatch/export/kafka-lag | POST | Lag charts for predefined consumer groups |
| /cloudwatch/export/kafka-time-lag | POST | Time-based lag charts |
| /cloudwatch/export/comprehensive-report | POST | Full chart set + metrics JSON directory |
| /cloudwatch/export/comprehensive-pdf-report | POST | PDF containing consolidated charts |
| /analysis/comprehensive-report | POST | Generate PDF + AI analysis (default/stability/custom) |
| /analysis/stability | POST | Vector data stability analysis + optional code extraction |
| /analysis/metrics-vector-data | GET | Return raw metrics vector JSON |
| /analysis/existing-report | POST | Analyze an existing PDF report |
| /analysis/performance-insights | POST | PDF + AI performance insights |
| /analysis/custom-analysis | POST | PDF + AI with custom prompt |

(Adjust consumer groups/topics in `CloudWatchChartService` as needed.)

### Sample Requests
#### Export Basic Charts (RDS)
```bash
curl -X POST "http://localhost:8080/cloudwatch/export?dbInstanceIdentifier=YOUR_RDS_INSTANCE&startTime=2025-10-24T00:00:00Z&endTime=2025-10-24T02:00:00Z"
```
Generates: `cpu_utilization.png`, `freeable_memory.png`.

#### Generate Comprehensive Report Directory
```bash
curl -X POST "http://localhost:8080/cloudwatch/export/comprehensive-report?dbInstanceIdentifier=YOUR_RDS_INSTANCE&startTime=2025-10-24T00:00:00Z&endTime=2025-10-24T06:00:00Z"
```
Creates timestamped directory with multiple Kafka & RDS charts + `metrics_vector_data.json`.

#### Stability Analysis with Code Extraction
```bash
curl -X POST "http://localhost:8080/analysis/stability?dbInstanceIdentifier=YOUR_RDS_INSTANCE&startTime=2025-10-24T00:00:00Z&endTime=2025-10-24T06:00:00Z"
```
Possible outputs:
- Stability analysis text
- Generated `StabilityAnalysisVisualization.java` (auto compiled & executed)
- Additional PNG charts produced by AI‑generated code

---
## How It Works (Flow)
1. Request hits REST controller.
2. `CloudWatchChartService` builds AWS `GetMetricStatisticsRequest` queries with appropriate dimensions.
3. Data points are sorted and added to `TimeSeries` objects -> assembled into datasets -> chart rendered.
4. Styling enhancements applied (color palette, averages subtitle, timezone labeling).
5. Charts saved as PNG; optionally combined into PDF (iText) and exported.
6. Vector JSON produced for numerical AI analysis (timestamps + average/max values).
7. AI payload assembled (prompt + data context) and POSTed; `task_id` returned.
8. `pollForCompletion()` polls status endpoint until Complete/Failed.
9. If AI response contains a fenced ```java block -> Extracted, wrapped/injected with metrics JSON, compiled, executed.
10. Output artifacts live under `comprehensive_report_<timestamp>/` for audit.

---
## Configuration
In `application.properties` (copy from sample):
- `aws.accessKey`, `aws.secretKey`, `aws.sessionToken` – temporary credentials.
- `ai.analysis.*` – model tuning (max tokens, temperature), endpoints, application tag.
- `chart.timezone` – influences date axis formatting.
- `ai.analysis.try-pdf-upload` – toggles base64 PDF upload vs text extraction fallback.
- `ai.analysis.ssl.trust-all-certificates` – only enable in non-production environments.

---
## Error Handling & Edge Cases
| Scenario | Handling |
|----------|---------|
| Missing metrics window | Returns empty/limited charts; may throw runtime error if no data found for dimension discovery. |
| AI task timeout | Throws runtime exception after max polls (default 5 minutes). |
| No Java fenced block | Logs warning, skips code generation silently. |
| PDF upload fails | Falls back to text extraction with disclaimer in prompt. |
| Vector JSON too large | Uses escaped string embedding; consider future streaming improvement. |

---
## Security Considerations
- Do NOT commit real AWS or AI credentials.
- Session tokens are short-lived; integrate refresh or assume role for production.
- Disable trust-all SSL in production.
- Limit AI model max tokens for cost/performance.

---
## Extensibility Ideas
- Add metrics aggregation across multiple RDS instances or Kafka clusters.
- Introduce Prometheus scraper / OpenTelemetry exporting.
- Implement pre-AI anomaly detection (z-score, Holt-Winters) to enrich prompts.
- Plug in alternative LLM providers via strategy pattern.
- Provide a lightweight UI (React/Vue) for browsing generated report directories.
- Replace static embedding with runtime file reading for generated Java code.

---
## Roadmap
- Unit tests for chart generation boundaries & AI integration mocks.
- Dockerfile + GitHub Actions CI/CD.
- Role-based access control and API keys for endpoints.
- Enhanced statistical summaries (percentiles, moving averages). 
- Streaming large JSON metrics to avoid memory spikes.

---
## Troubleshooting
| Symptom | Suggestion |
|---------|------------|
| Empty charts | Verify time window & UTC formatting; check CloudWatch data availability. |
| AI "Failed" status | Confirm credentials, endpoint URLs, payload size, and logs. |
| Generated code compile errors | Inspect saved `StabilityAnalysisVisualization.java`; adjust prompt or add missing imports. |
| Slow AI responses | Reduce prompt size, lower `max_tokens`, adjust temperature. |
| Memory pressure | Limit metrics window or add pagination in future iteration. |

---
## Recruiter Highlights
- AWS SDK v2 dynamic dimension discovery (topics, brokers, consumer groups).
- Time-series visualization with professional styling & PDF reporting.
- AI workflow integration and runtime code generation/execution pipeline.
- Clear separation of concerns; production-ready error handling patterns.
- Demonstrates ability to blend observability, data analysis, and developer tooling.

---
## Suggested Future Improvements
- Switch generated code to load JSON from file instead of embedding large strings.
- Add test harness for `extractAndSaveJavaCode` to validate various AI response shapes.
- Implement metrics caching & batching to reduce AWS API calls.
- Graceful degradation when certain AWS namespaces are unavailable.

---
## License
Add a LICENSE file (recommended: MIT):
```
MIT License (c) 2025 YOUR NAME
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction...
```
(Replace with full MIT text.)

---
## Quick Command Reference
```bash
# Build
mvn clean compile

# Run
mvn spring-boot:run

# Example stability analysis
curl -X POST "http://localhost:8080/analysis/stability?dbInstanceIdentifier=YOUR_RDS_INSTANCE&startTime=2025-10-24T00:00:00Z&endTime=2025-10-24T06:00:00Z"

# Fetch raw metrics vector data
curl -X GET "http://localhost:8080/analysis/metrics-vector-data?dbInstanceIdentifier=YOUR_RDS_INSTANCE&startTime=2025-10-24T00:00:00Z&endTime=2025-10-24T06:00:00Z"
```

---
## Contributing
Open to improvements: PRs for tests, performance enhancements, or new metric types are welcome.

---
## Contact
For questions or collaboration opportunities: YOUR_EMAIL or LinkedIn/GitHub profile.

