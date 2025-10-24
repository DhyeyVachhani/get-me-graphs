package com.getmegraphs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cloudwatch")
public class CloudWatchChartController {
    private static final Logger logger = LoggerFactory.getLogger(CloudWatchChartController.class);

    private final CloudWatchChartService chartService;

    public CloudWatchChartController(CloudWatchChartService chartService) {
        this.chartService = chartService;
    }

    @PostMapping("/export")
    public String exportCharts(
            @RequestParam String dbInstanceIdentifier,
            @RequestParam String startTime,
            @RequestParam String endTime
    ) {
        try {
            chartService.plotAndSaveMetric(dbInstanceIdentifier, "CPUUtilization", startTime, endTime, "CPU Utilization (%)", "cpu_utilization.png");
            chartService.plotAndSaveMetric(dbInstanceIdentifier, "FreeableMemory", startTime, endTime, "Freeable Memory (MB)", "freeable_memory.png");
            return "Charts exported successfully!";
        } catch (Exception e) {
            logger.error("Error exporting charts for DB instance: {}", dbInstanceIdentifier, e);
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/export/database-connections")
    public String exportDatabaseConnectionsChart(
            @RequestParam String dbInstanceIdentifier,
            @RequestParam String startTime,
            @RequestParam String endTime
    ) {
        try {
            chartService.plotAndSaveMetric(dbInstanceIdentifier, "DatabaseConnections", startTime, endTime, "Database Connections (Count)", "database_connections.png");
            return "Database connections chart exported successfully!";
        } catch (Exception e) {
            logger.error("Error exporting database connections chart for DB instance: {}", dbInstanceIdentifier, e);
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/export/kafka-consumer-lag")
    public String exportKafkaConsumerLagChart(
            @RequestParam String clusterName,
            @RequestParam String consumerGroup,
            @RequestParam String topic,
            @RequestParam String startTime,
            @RequestParam String endTime
    ) {
        try {
            chartService.plotAndSaveKafkaMetric(clusterName, consumerGroup, topic, "ConsumerLag", startTime, endTime, "Consumer Lag (Messages)", "kafka_consumer_lag.png");
            return "Kafka consumer lag chart exported successfully!";
        } catch (Exception e) {
            logger.error("Error exporting Kafka consumer lag chart for cluster: {}, consumerGroup: {}, topic: {}", clusterName, consumerGroup, topic, e);
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/export/kafka-cpu-usage-by-broker")
    public String exportKafkaCpuUsageByBrokerChart(
            @RequestParam String clusterName,
            @RequestParam String startTime,
            @RequestParam String endTime
    ) {
        try {
            chartService.plotAndSaveKafkaBrokerMetric(clusterName, "CpuSystem", startTime, endTime, "CPU Usage (%)", "kafka_cpu_usage_by_broker.png");
            return "Kafka CPU usage by broker chart exported successfully!";
        } catch (Exception e) {
            logger.error("Error exporting Kafka CPU usage by broker chart for cluster: {}", clusterName, e);
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/export/comprehensive-report")
    public String exportComprehensiveReport(
            @RequestParam(required = false) String clusterName,
            @RequestParam(required = false) String dbInstanceIdentifier,
            @RequestParam String startTime,
            @RequestParam String endTime
    ) {
        try {
            String reportPath = chartService.generateComprehensiveReport(clusterName, dbInstanceIdentifier, startTime, endTime);
            return "Comprehensive report generated successfully at: " + reportPath;
        } catch (Exception e) {
            logger.error("Error generating comprehensive report for cluster: {}, DB instance: {}", clusterName, dbInstanceIdentifier, e);
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/export/database-read-iops")
    public String exportDatabaseReadIOPSChart(
            @RequestParam String dbInstanceIdentifier,
            @RequestParam String startTime,
            @RequestParam String endTime
    ) {
        try {
            chartService.plotAndSaveMetric(dbInstanceIdentifier, "ReadIOPS", startTime, endTime, "Read IOPS (Operations/Second)", "database_read_iops.png");
            return "Database Read IOPS chart exported successfully!";
        } catch (Exception e) {
            logger.error("Error exporting database Read IOPS chart for DB instance: {}", dbInstanceIdentifier, e);
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/export/database-write-iops")
    public String exportDatabaseWriteIOPSChart(
            @RequestParam String dbInstanceIdentifier,
            @RequestParam String startTime,
            @RequestParam String endTime
    ) {
        try {
            chartService.plotAndSaveMetric(dbInstanceIdentifier, "WriteIOPS", startTime, endTime, "Write IOPS (Operations/Second)", "database_write_iops.png");
            return "Database Write IOPS chart exported successfully!";
        } catch (Exception e) {
            logger.error("Error exporting database Write IOPS chart for DB instance: {}", dbInstanceIdentifier, e);
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/export/comprehensive-pdf-report")
    public String exportComprehensivePDFReport(
            @RequestParam(required = false) String clusterName,
            @RequestParam(required = false) String dbInstanceIdentifier,
            @RequestParam String startTime,
            @RequestParam String endTime
    ) {
        try {
            String pdfPath = chartService.generatePDFReport(clusterName, dbInstanceIdentifier, startTime, endTime);
            return "Comprehensive PDF report generated successfully at: " + pdfPath;
        } catch (Exception e) {
            logger.error("Error generating comprehensive PDF report for cluster: {}, DB instance: {}", clusterName, dbInstanceIdentifier, e);
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/export/kafka-lag")
    public String exportKafkaLag(
            @RequestParam String clusterName,
            @RequestParam String startTime,
            @RequestParam String endTime
    ) {
        try {
            String outputFileName = "kafka_lag_" + clusterName.replaceAll("[^a-zA-Z0-9]", "_") + ".png";
            chartService.plotAndSaveKafkaLagLimited(clusterName, startTime, endTime, outputFileName);
            return "Kafka lag charts (SumOffsetLag & MaxOffsetLag) for 3 consumer groups exported successfully. Check files: " +
                   "kafka_lag_" + clusterName.replaceAll("[^a-zA-Z0-9]", "_") + "_worker.png, " +
                   "kafka_lag_" + clusterName.replaceAll("[^a-zA-Z0-9]", "_") + "_notify.png, " +
                   "kafka_lag_" + clusterName.replaceAll("[^a-zA-Z0-9]", "_") + "_async_notify.png";
        } catch (Exception e) {
            logger.error("Error exporting Kafka lag charts for cluster: {}", clusterName, e);
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/export/kafka-time-lag")
    public String exportKafkaTimeLag(
            @RequestParam String clusterName,
            @RequestParam String startTime,
            @RequestParam String endTime
    ) {
        try {
            String outputFileName = "kafka_time_lag_" + clusterName.replaceAll("[^a-zA-Z0-9]", "_") + ".png";
            chartService.plotAndSaveKafkaTimeLagLimited(clusterName, startTime, endTime, outputFileName);
            return "Kafka time-based lag charts (RollingEstimatedTimeLagMax & EstimatedMaxTimeLag) for 3 consumer groups exported successfully. Check files: " +
                   "kafka_time_lag_" + clusterName.replaceAll("[^a-zA-Z0-9]", "_") + "_worker.png, " +
                   "kafka_time_lag_" + clusterName.replaceAll("[^a-zA-Z0-9]", "_") + "_notify.png, " +
                   "kafka_time_lag_" + clusterName.replaceAll("[^a-zA-Z0-9]", "_") + "_async_notify.png";
        } catch (Exception e) {
            logger.error("Error exporting Kafka time lag charts for cluster: {}", clusterName, e);
            return "Error: " + e.getMessage();
        }
    }

}

