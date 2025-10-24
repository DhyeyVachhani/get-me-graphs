package com.getmegraphs;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Datapoint;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.DimensionFilter;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;
import software.amazon.awssdk.services.cloudwatch.model.ListMetricsRequest;
import software.amazon.awssdk.services.cloudwatch.model.ListMetricsResponse;
import software.amazon.awssdk.services.cloudwatch.model.Statistic;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;


@Service
public class CloudWatchChartService {

    private static final Logger logger = LoggerFactory.getLogger(CloudWatchChartService.class);
    private final CloudWatchClient cloudWatchClient;

    @Value("${chart.timezone:UTC}")
    private String chartTimezone;

    // Sanitized placeholder consumer group/topic pairs (replace via configuration if needed)
    private static final String[][] DEFAULT_CONSUMER_GROUP_TOPIC_PAIRS = {
            {"worker-consumer-group", "worker-topic"},
            {"notify-consumer-group", "notify-topic"},
            {"async-notify-consumer-group", "async-notify-topic"}
    };

    public CloudWatchChartService(
            @Value("${aws.accessKey}") String accessKey,
            @Value("${aws.secretKey}") String secretKey,
            @Value("${aws.sessionToken}") String sessionToken) {
        AwsSessionCredentials awsCreds = AwsSessionCredentials.create(accessKey, secretKey, sessionToken);
        this.cloudWatchClient = CloudWatchClient.builder()
                .region(Region.of(String.valueOf(Region.EU_WEST_1)))
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();
    }

    private JFreeChart enhanceChart(JFreeChart chart, String title, String yAxisLabel, boolean isKafkaChart) {
        // Set modern color scheme
        chart.setBackgroundPaint(new Color(248, 249, 250));
        chart.setBorderVisible(false);

        // Enhance title with timezone information
        String titleWithTimezone = title + " (Time: " + chartTimezone + ")";
        TextTitle chartTitle = new TextTitle(titleWithTimezone, new java.awt.Font("Arial", java.awt.Font.BOLD, 16));
        chartTitle.setPaint(new Color(33, 37, 41));
        chart.setTitle(chartTitle);

        // Get the plot
        XYPlot plot = (XYPlot) chart.getPlot();

        // Calculate and add average values subtitle
        String averageText = calculateAverageText(plot);
        if (!averageText.isEmpty()) {
            TextTitle averageTitle = new TextTitle(averageText, new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));
            averageTitle.setPaint(new Color(73, 80, 87));
            averageTitle.setPosition(RectangleEdge.BOTTOM);
            chart.addSubtitle(averageTitle);
        }

        // Set background colors with subtle gradients
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(new Color(240, 242, 245));
        plot.setRangeGridlinePaint(new Color(240, 242, 245));
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinesVisible(true);
        plot.setOutlineVisible(false);

        // Add subtle shadow effect
        plot.setShadowGenerator(null);

        // Configure renderer
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

        // Define professional color palette for different series
        Color[] colors = {
                new Color(0, 123, 255),    // Professional Blue
                new Color(220, 53, 69),    // Professional Red
                new Color(40, 167, 69),    // Professional Green
                new Color(255, 193, 7),    // Professional Amber
                new Color(102, 16, 242),   // Professional Purple
                new Color(253, 126, 20)    // Professional Orange
        };

        // Apply colors and styling to each series
        for (int i = 0; i < plot.getDataset().getSeriesCount(); i++) {
            Color seriesColor = colors[i % colors.length];

            // Line styling with optimal thickness for visibility and overlap detection
            renderer.setSeriesPaint(i, seriesColor);
            renderer.setSeriesStroke(i, new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // Disable shape markers for cleaner lines
            renderer.setSeriesShapesVisible(i, false);
        }

        plot.setRenderer(renderer);

        // Configure axes
        DateAxis domainAxis = (DateAxis) plot.getDomainAxis();
        SimpleDateFormat axisDateFormat = new SimpleDateFormat("HH:mm");
        axisDateFormat.setTimeZone(java.util.TimeZone.getTimeZone(chartTimezone));
        domainAxis.setDateFormatOverride(axisDateFormat);
        domainAxis.setLabelFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));
        domainAxis.setTickLabelFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 10));
        domainAxis.setLabelPaint(new Color(73, 80, 87));
        domainAxis.setTickLabelPaint(new Color(73, 80, 87));

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setLabel(yAxisLabel);
        rangeAxis.setLabelFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));
        rangeAxis.setTickLabelFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 10));
        rangeAxis.setLabelPaint(new Color(73, 80, 87));
        rangeAxis.setTickLabelPaint(new Color(73, 80, 87));

        // Add padding
        plot.setInsets(new RectangleInsets(10, 10, 10, 10));

        // Configure legend if present
        if (chart.getLegend() != null) {
            chart.getLegend().setItemFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 11));
            chart.getLegend().setBackgroundPaint(new Color(248, 249, 250));
            chart.getLegend().setBorder(0, 0, 0, 0);
        }

        return chart;
    }

    private String calculateAverageText(XYPlot plot) {
        StringBuilder averageText = new StringBuilder("Average Values: ");
        TimeSeriesCollection dataset = (TimeSeriesCollection) plot.getDataset();

        if (dataset == null || dataset.getSeriesCount() == 0) {
            return "";
        }

        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            TimeSeries series = dataset.getSeries(i);
            if (series.getItemCount() > 0) {
                double sum = 0.0;
                int count = 0;

                for (int j = 0; j < series.getItemCount(); j++) {
                    Number value = series.getValue(j);
                    if (value != null) {
                        sum += value.doubleValue();
                        count++;
                    }
                }

                if (count > 0) {
                    double average = sum / count;
                    if (i > 0) {
                        averageText.append(", ");
                    }
                    averageText.append(series.getKey()).append(": ").append(String.format("%.2f", average));
                }
            }
        }

        return averageText.toString();
    }

    public void plotAndSaveMetric(String dbInstanceIdentifier,
                                  String metricName,
                                  String startTimeStr,
                                  String endTimeStr,
                                  String yAxisLabel,
                                  String outputFileName) throws Exception {
        Instant startTime = Instant.parse(startTimeStr);
        Instant endTime = Instant.parse(endTimeStr);

        Dimension dimension = Dimension.builder()
                .name("DBInstanceIdentifier")
                .value(dbInstanceIdentifier)
                .build();

        GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                .namespace("AWS/RDS")
                .metricName(metricName)
                .dimensions(dimension)
                .startTime(startTime)
                .endTime(endTime)
                .period(60) // 1 minute
                .statistics(Statistic.MAXIMUM, Statistic.AVERAGE)
                .build();

        GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(request);

        List<Datapoint> datapoints = new ArrayList<>(response.datapoints());
        datapoints.sort((a, b) -> a.timestamp().compareTo(b.timestamp()));

        TimeSeries series = new TimeSeries(metricName);

        for (Datapoint dp : datapoints) {
            double value = dp.average() != null ? dp.average() : 0.0;
            if (metricName.equals("FreeableMemory")) {
                value = value / (1024 * 1024); // Convert bytes to MB
            }
            ZonedDateTime zdt = dp.timestamp().atZone(ZoneId.of("UTC"));
            series.addOrUpdate(new Millisecond(java.util.Date.from(zdt.toInstant())), value);
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(series);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                metricName + " for " + dbInstanceIdentifier,
                "Time",
                yAxisLabel,
                dataset,
                false,
                false,
                false
        );

        // Apply enhanced styling
        chart = enhanceChart(chart, metricName + " for " + dbInstanceIdentifier, yAxisLabel, false);

        logger.info("Saved chart: {}", outputFileName);
        ChartUtils.saveChartAsPNG(new File(outputFileName), chart, 1200, 600);

    }

    public void plotAndSaveKafkaMetric(String clusterName,
                                       String consumerGroup,
                                       String topic,
                                       String metricName,
                                       String startTimeStr,
                                       String endTimeStr,
                                       String yAxisLabel,
                                       String outputFileName) throws Exception {
        Instant startTime = Instant.parse(startTimeStr);
        Instant endTime = Instant.parse(endTimeStr);

        List<Dimension> dimensions = new ArrayList<>();
        dimensions.add(Dimension.builder()
                .name("Cluster Name")
                .value(clusterName)
                .build());
        dimensions.add(Dimension.builder()
                .name("Consumer Group")
                .value(consumerGroup)
                .build());
        dimensions.add(Dimension.builder()
                .name("Topic")
                .value(topic)
                .build());

        GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                .namespace("AWS/Kafka")
                .metricName(metricName)
                .dimensions(dimensions)
                .startTime(startTime)
                .endTime(endTime)
                .period(60) // 1 minute
                .statistics(Statistic.MAXIMUM, Statistic.AVERAGE)
                .build();

        GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(request);

        List<Datapoint> datapoints = new ArrayList<>(response.datapoints());
        datapoints.sort((a, b) -> a.timestamp().compareTo(b.timestamp()));

        TimeSeries series = new TimeSeries(metricName + " - " + consumerGroup);

        for (Datapoint dp : datapoints) {
            double value = dp.average() != null ? dp.average() : 0.0;
            ZonedDateTime zdt = dp.timestamp().atZone(ZoneId.of("UTC"));
            series.addOrUpdate(new Millisecond(java.util.Date.from(zdt.toInstant())), value);
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(series);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                metricName + " for " + consumerGroup + " on " + topic,
                "Time",
                yAxisLabel,
                dataset,
                false,
                false,
                false
        );

        // Apply enhanced styling
        chart = enhanceChart(chart, metricName + " for " + consumerGroup + " on " + topic, yAxisLabel, true);

        ChartUtils.saveChartAsPNG(new File(outputFileName), chart, 1200, 600);
        logger.info("Saved Kafka chart: {}", outputFileName);
    }

    public void plotAndSaveKafkaMetricAllTopics(String clusterName,
                                                String consumerGroup,
                                                String[] topics,
                                                String metricName,
                                                String startTimeStr,
                                                String endTimeStr,
                                                String yAxisLabel,
                                                String outputFileName) throws Exception {
        Instant startTime = Instant.parse(startTimeStr);
        Instant endTime = Instant.parse(endTimeStr);

        TimeSeriesCollection dataset = new TimeSeriesCollection();

        for (String topic : topics) {
            List<Dimension> dimensions = new ArrayList<>();
            dimensions.add(Dimension.builder()
                    .name("Cluster Name")
                    .value(clusterName)
                    .build());
            dimensions.add(Dimension.builder()
                    .name("Consumer Group")
                    .value(consumerGroup)
                    .build());
            dimensions.add(Dimension.builder()
                    .name("Topic")
                    .value(topic)
                    .build());

            GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                    .namespace("AWS/Kafka")
                    .metricName(metricName)
                    .dimensions(dimensions)
                    .startTime(startTime)
                    .endTime(endTime)
                    .period(60) // 1 minute
                    .statistics(Statistic.MAXIMUM, Statistic.AVERAGE)
                    .build();

            GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(request);

            List<Datapoint> datapoints = new ArrayList<>(response.datapoints());
            datapoints.sort((a, b) -> a.timestamp().compareTo(b.timestamp()));

            TimeSeries series = new TimeSeries(topic);

            for (Datapoint dp : datapoints) {
                double value = dp.average() != null ? dp.average() : 0.0;
                ZonedDateTime zdt = dp.timestamp().atZone(ZoneId.of("UTC"));
                series.addOrUpdate(new Millisecond(java.util.Date.from(zdt.toInstant())), value);
            }

            dataset.addSeries(series);
        }

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                metricName + " for Consumer Group: " + consumerGroup,
                "Time",
                yAxisLabel,
                dataset,
                true, // Show legend to distinguish topics
                false,
                false
        );

        // Apply enhanced styling
        chart = enhanceChart(chart, metricName + " for Consumer Group: " + consumerGroup, yAxisLabel, true);

        ChartUtils.saveChartAsPNG(new File(outputFileName), chart, 1200, 600);
        logger.info("Saved Kafka all topics chart: {}", outputFileName);
    }

    public void plotAndSaveKafkaMetricAutoDiscover(String clusterName,
                                                   String consumerGroup,
                                                   String metricName,
                                                   String startTimeStr,
                                                   String endTimeStr,
                                                   String yAxisLabel,
                                                   String outputFileName) throws Exception {
        Instant startTime = Instant.parse(startTimeStr);
        Instant endTime = Instant.parse(endTimeStr);

        // First, discover all topics for this consumer group by listing metrics
        ListMetricsRequest listMetricsRequest = ListMetricsRequest.builder()
                .namespace("AWS/Kafka")
                .metricName(metricName)
                .dimensions(
                        DimensionFilter.builder()
                                .name("Cluster Name")
                                .value(clusterName)
                                .build(),
                        DimensionFilter.builder()
                                .name("Consumer Group")
                                .value(consumerGroup)
                                .build()
                )
                .build();

        ListMetricsResponse listMetricsResponse = cloudWatchClient.listMetrics(listMetricsRequest);

        TimeSeriesCollection dataset = new TimeSeriesCollection();

        // Extract unique topics from the metrics
        List<String> discoveredTopics = listMetricsResponse.metrics().stream()
                .flatMap(metric -> metric.dimensions().stream())
                .filter(dimension -> "Topic".equals(dimension.name()))
                .map(Dimension::value)
                .distinct()
                .sorted()
                .toList();

        logger.info("Discovered topics: {}", discoveredTopics);

        if (discoveredTopics.isEmpty()) {
            throw new RuntimeException("No topics found for consumer group: " + consumerGroup + " in cluster: " + clusterName);
        }

        // Fetch data for each discovered topic
        for (String topic : discoveredTopics) {
            List<Dimension> dimensions = new ArrayList<>();
            dimensions.add(Dimension.builder()
                    .name("Cluster Name")
                    .value(clusterName)
                    .build());
            dimensions.add(Dimension.builder()
                    .name("Consumer Group")
                    .value(consumerGroup)
                    .build());
            dimensions.add(Dimension.builder()
                    .name("Topic")
                    .value(topic)
                    .build());

            GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                    .namespace("AWS/Kafka")
                    .metricName(metricName)
                    .dimensions(dimensions)
                    .startTime(startTime)
                    .endTime(endTime)
                    .period(60) // 1 minutes
                    .statistics(Statistic.MAXIMUM, Statistic.AVERAGE)
                    .build();

            try {
                GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(request);

                List<Datapoint> datapoints = new ArrayList<>(response.datapoints());
                if (!datapoints.isEmpty()) {
                    datapoints.sort((a, b) -> a.timestamp().compareTo(b.timestamp()));

                    TimeSeries series = new TimeSeries(topic);

                    for (Datapoint dp : datapoints) {
                        double value = dp.average() != null ? dp.average() : 0.0;
                        ZonedDateTime zdt = dp.timestamp().atZone(ZoneId.of("UTC"));
                        series.addOrUpdate(new Millisecond(java.util.Date.from(zdt.toInstant())), value);
                    }

                    dataset.addSeries(series);
                }
            } catch (Exception e) {
                logger.error("Failed to fetch data for topic: {}, error: {}", topic, e.getMessage(),e);
            }
        }

        if (dataset.getSeriesCount() == 0) {
            throw new RuntimeException("No data found for any topics in consumer group: " + consumerGroup);
        }

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                metricName + " for Consumer Group: " + consumerGroup + " (Auto-discovered)",
                "Time",
                yAxisLabel,
                dataset,
                true, // Show legend to distinguish topics
                false,
                false
        );

        // Apply enhanced styling
        chart = enhanceChart(chart, metricName + " for Consumer Group: " + consumerGroup + " (Auto-discovered)", yAxisLabel, true);

        ChartUtils.saveChartAsPNG(new File(outputFileName), chart, 1200, 600);
        logger.info("Saved Kafka auto-discovered topics chart: {} with {} topics", outputFileName, dataset.getSeriesCount());
    }

    public void plotAndSaveKafkaMetricAllConsumerGroups(String clusterName,
                                                        String metricName,
                                                        String startTimeStr,
                                                        String endTimeStr,
                                                        String yAxisLabel,
                                                        String outputFileName) throws Exception {
        Instant startTime = Instant.parse(startTimeStr);
        Instant endTime = Instant.parse(endTimeStr);

        // First, discover all consumer groups for this cluster by listing metrics
        ListMetricsRequest listMetricsRequest = ListMetricsRequest.builder()
                .namespace("AWS/Kafka")
                .metricName(metricName)
                .dimensions(
                        DimensionFilter.builder()
                                .name("Cluster Name")
                                .value(clusterName)
                                .build()
                )
                .build();

        ListMetricsResponse listMetricsResponse = cloudWatchClient.listMetrics(listMetricsRequest);

        TimeSeriesCollection dataset = new TimeSeriesCollection();

        // Extract unique consumer groups from the metrics
        List<String> discoveredConsumerGroups = listMetricsResponse.metrics().stream()
                .flatMap(metric -> metric.dimensions().stream())
                .filter(dimension -> "Consumer Group".equals(dimension.name()))
                .map(Dimension::value)
                .distinct()
                .sorted()
                .toList();

        logger.info("Discovered consumer groups: {}", discoveredConsumerGroups);

        if (discoveredConsumerGroups.isEmpty()) {
            throw new RuntimeException("No consumer groups found for cluster: " + clusterName);
        }

        // For each consumer group, aggregate lag across all topics
        for (String consumerGroup : discoveredConsumerGroups) {
            // Get all topics for this consumer group
            ListMetricsRequest topicsRequest = ListMetricsRequest.builder()
                    .namespace("AWS/Kafka")
                    .metricName(metricName)
                    .dimensions(
                            DimensionFilter.builder()
                                    .name("Cluster Name")
                                    .value(clusterName)
                                    .build(),
                            DimensionFilter.builder()
                                    .name("Consumer Group")
                                    .value(consumerGroup)
                                    .build()
                    )
                    .build();

            ListMetricsResponse topicsResponse = cloudWatchClient.listMetrics(topicsRequest);

            List<String> topics = topicsResponse.metrics().stream()
                    .flatMap(metric -> metric.dimensions().stream())
                    .filter(dimension -> "Topic".equals(dimension.name()))
                    .map(Dimension::value)
                    .distinct()
                    .sorted()
                    .toList();

            TimeSeries consumerGroupSeries = new TimeSeries(consumerGroup + " (Total)");

            // For each topic in this consumer group, fetch data and aggregate
            for (String topic : topics) {
                List<Dimension> dimensions = new ArrayList<>();
                dimensions.add(Dimension.builder()
                        .name("Cluster Name")
                        .value(clusterName)
                        .build());
                dimensions.add(Dimension.builder()
                        .name("Consumer Group")
                        .value(consumerGroup)
                        .build());
                dimensions.add(Dimension.builder()
                        .name("Topic")
                        .value(topic)
                        .build());

                GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                        .namespace("AWS/Kafka")
                        .metricName(metricName)
                        .dimensions(dimensions)
                        .startTime(startTime)
                        .endTime(endTime)
                        .period(60) // 1 minutes
                        .statistics(Statistic.MAXIMUM, Statistic.AVERAGE)
                        .build();

                try {
                    GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(request);
                    List<Datapoint> datapoints = new ArrayList<>(response.datapoints());

                    if (!datapoints.isEmpty()) {
                        datapoints.sort((a, b) -> a.timestamp().compareTo(b.timestamp()));

                        // Aggregate data points for this consumer group
                        for (Datapoint dp : datapoints) {
                            double value = dp.average() != null ? dp.average() : 0.0;
                            ZonedDateTime zdt = dp.timestamp().atZone(ZoneId.of("UTC"));
                            Millisecond timePoint = new Millisecond(java.util.Date.from(zdt.toInstant()));

                            // Add to existing value or create new one
                            Number existingValue = consumerGroupSeries.getValue(timePoint);
                            double newValue = (existingValue != null ? existingValue.doubleValue() : 0.0) + value;
                            consumerGroupSeries.addOrUpdate(timePoint, newValue);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to fetch data for consumer group: {}, topic: {}", consumerGroup, topic, e);
                }
            }

            // Only add series if it has data
            if (consumerGroupSeries.getItemCount() > 0) {
                dataset.addSeries(consumerGroupSeries);
            }
        }

        if (dataset.getSeriesCount() == 0) {
            throw new RuntimeException("No data found for any consumer groups in cluster: " + clusterName);
        }

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                metricName + " for All Consumer Groups in Cluster: " + clusterName,
                "Time",
                yAxisLabel,
                dataset,
                true, // Show legend to distinguish consumer groups
                false,
                false
        );

        // Apply enhanced styling
        chart = enhanceChart(chart, metricName + " for All Consumer Groups in Cluster: " + clusterName, yAxisLabel, true);

        ChartUtils.saveChartAsPNG(new File(outputFileName), chart, 1200, 600);
        logger.info("Saved Kafka all consumer groups chart: {} with {} consumer groups", outputFileName, dataset.getSeriesCount());
    }

    public void plotAndSaveKafkaBrokerMetric(String clusterName,
                                             String metricName,
                                             String startTimeStr,
                                             String endTimeStr,
                                             String yAxisLabel,
                                             String outputFileName) throws Exception {
        Instant startTime = Instant.parse(startTimeStr);
        Instant endTime = Instant.parse(endTimeStr);

        // First, discover all brokers for this cluster by listing metrics
        ListMetricsRequest listMetricsRequest = ListMetricsRequest.builder()
                .namespace("AWS/Kafka")
                .metricName(metricName)
                .dimensions(
                        DimensionFilter.builder()
                                .name("Cluster Name")
                                .value(clusterName)
                                .build()
                )
                .build();

        ListMetricsResponse listMetricsResponse = cloudWatchClient.listMetrics(listMetricsRequest);

        TimeSeriesCollection dataset = new TimeSeriesCollection();

        // Extract unique broker IDs from the metrics
        List<String> discoveredBrokers = listMetricsResponse.metrics().stream()
                .flatMap(metric -> metric.dimensions().stream())
                .filter(dimension -> "Broker ID".equals(dimension.name()))
                .map(Dimension::value)
                .distinct()
                .sorted((a, b) -> {
                    try {
                        return Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
                    } catch (NumberFormatException e) {
                        return a.compareTo(b);
                    }
                })
                .toList();

        logger.info("Discovered brokers: {}", discoveredBrokers);

        if (discoveredBrokers.isEmpty()) {
            throw new RuntimeException("No brokers found for cluster: " + clusterName);
        }

        // Fetch data for each discovered broker
        for (String brokerId : discoveredBrokers) {
            List<Dimension> dimensions = new ArrayList<>();
            dimensions.add(Dimension.builder()
                    .name("Cluster Name")
                    .value(clusterName)
                    .build());
            dimensions.add(Dimension.builder()
                    .name("Broker ID")
                    .value(brokerId)
                    .build());

            GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                    .namespace("AWS/Kafka")
                    .metricName(metricName)
                    .dimensions(dimensions)
                    .startTime(startTime)
                    .endTime(endTime)
                    .period(60) // 1 minute
                    .statistics(Statistic.MAXIMUM, Statistic.AVERAGE)
                    .build();

            try {
                GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(request);

                List<Datapoint> datapoints = new ArrayList<>(response.datapoints());
                if (!datapoints.isEmpty()) {
                    datapoints.sort((a, b) -> a.timestamp().compareTo(b.timestamp()));

                    TimeSeries series = new TimeSeries("Broker " + brokerId);

                    for (Datapoint dp : datapoints) {
                        double value = dp.average() != null ? dp.average() : 0.0;
                        // CpuSystem metric from AWS Kafka is already in percentage format (0-100)
                        // No conversion needed
                        ZonedDateTime zdt = dp.timestamp().atZone(ZoneId.of("UTC"));
                        series.addOrUpdate(new Millisecond(java.util.Date.from(zdt.toInstant())), value);
                    }

                    dataset.addSeries(series);
                }
            } catch (Exception e) {
                logger.error("Failed to fetch data for broker: {}", brokerId, e);
            }
        }

        if (dataset.getSeriesCount() == 0) {
            throw new RuntimeException("No data found for any brokers in cluster: " + clusterName);
        }

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                metricName + " by Broker for Cluster: " + clusterName,
                "Time",
                yAxisLabel,
                dataset,
                true, // Show legend to distinguish brokers
                false,
                false
        );

        // Apply enhanced styling
        chart = enhanceChart(chart, metricName + " by Broker for Cluster: " + clusterName, yAxisLabel, true);

        ChartUtils.saveChartAsPNG(new File(outputFileName), chart, 1200, 600);
        logger.info("Saved Kafka broker metric chart: {} with {} brokers", outputFileName, dataset.getSeriesCount());
    }

    public void plotAndSaveKafkaLagByTopics(String clusterName,
                                            String startTimeStr,
                                            String endTimeStr,
                                            String yAxisLabel,
                                            String outputFileName) throws Exception {
        Instant startTime = Instant.parse(startTimeStr);
        Instant endTime = Instant.parse(endTimeStr);

        // First, discover all topics in the cluster by listing ConsumerLag metrics
        ListMetricsRequest listMetricsRequest = ListMetricsRequest.builder()
                .namespace("AWS/Kafka")
                .metricName("ConsumerLag")
                .dimensions(
                        DimensionFilter.builder()
                                .name("Cluster Name")
                                .value(clusterName)
                                .build()
                )
                .build();

        ListMetricsResponse listMetricsResponse = cloudWatchClient.listMetrics(listMetricsRequest);

        TimeSeriesCollection dataset = new TimeSeriesCollection();

        // Extract unique topics from the metrics
        List<String> discoveredTopics = listMetricsResponse.metrics().stream()
                .flatMap(metric -> metric.dimensions().stream())
                .filter(dimension -> "Topic".equals(dimension.name()))
                .map(Dimension::value)
                .distinct()
                .sorted()
                .toList();

        logger.info("Discovered topics: {}", discoveredTopics);

        if (discoveredTopics.isEmpty()) {
            throw new RuntimeException("No topics found for cluster: " + clusterName);
        }

        // For each topic, aggregate lag across all consumer groups
        for (String topic : discoveredTopics) {
            // Get all consumer groups for this topic
            ListMetricsRequest consumerGroupsRequest = ListMetricsRequest.builder()
                    .namespace("AWS/Kafka")
                    .metricName("ConsumerLag")
                    .dimensions(
                            DimensionFilter.builder()
                                    .name("Cluster Name")
                                    .value(clusterName)
                                    .build(),
                            DimensionFilter.builder()
                                    .name("Topic")
                                    .value(topic)
                                    .build()
                    )
                    .build();

            ListMetricsResponse consumerGroupsResponse = cloudWatchClient.listMetrics(consumerGroupsRequest);

            List<String> consumerGroups = consumerGroupsResponse.metrics().stream()
                    .flatMap(metric -> metric.dimensions().stream())
                    .filter(dimension -> "Consumer Group".equals(dimension.name()))
                    .map(Dimension::value)
                    .distinct()
                    .sorted()
                    .toList();

            TimeSeries topicSeries = new TimeSeries(topic + " (Total)");

            // For each consumer group consuming this topic, fetch data and aggregate
            for (String consumerGroup : consumerGroups) {
                List<Dimension> dimensions = new ArrayList<>();
                dimensions.add(Dimension.builder()
                        .name("Cluster Name")
                        .value(clusterName)
                        .build());
                dimensions.add(Dimension.builder()
                        .name("Consumer Group")
                        .value(consumerGroup)
                        .build());
                dimensions.add(Dimension.builder()
                        .name("Topic")
                        .value(topic)
                        .build());

                GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                        .namespace("AWS/Kafka")
                        .metricName("ConsumerLag")
                        .dimensions(dimensions)
                        .startTime(startTime)
                        .endTime(endTime)
                        .period(60) // 1 minute
                        .statistics(Statistic.MAXIMUM, Statistic.AVERAGE)
                        .build();

                try {
                    GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(request);
                    List<Datapoint> datapoints = new ArrayList<>(response.datapoints());

                    if (!datapoints.isEmpty()) {
                        datapoints.sort((a, b) -> a.timestamp().compareTo(b.timestamp()));

                        // Aggregate data points for this topic
                        for (Datapoint dp : datapoints) {
                            double value = dp.average() != null ? dp.average() : 0.0;
                            ZonedDateTime zdt = dp.timestamp().atZone(ZoneId.of("UTC"));
                            Millisecond timePoint = new Millisecond(java.util.Date.from(zdt.toInstant()));

                            // Add to existing value or create new one
                            Number existingValue = topicSeries.getValue(timePoint);
                            double newValue = (existingValue != null ? existingValue.doubleValue() : 0.0) + value;
                            topicSeries.addOrUpdate(timePoint, newValue);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to fetch data for topic: {}, consumer group: {}", topic, consumerGroup, e);
                }
            }

            // Only add series if it has data
            if (topicSeries.getItemCount() > 0) {
                dataset.addSeries(topicSeries);
            }
        }

        if (dataset.getSeriesCount() == 0) {
            throw new RuntimeException("No data found for any topics in cluster: " + clusterName);
        }

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Consumer Lag by Topics for Cluster: " + clusterName,
                "Time",
                yAxisLabel,
                dataset,
                true, // Show legend to distinguish topics
                false,
                false
        );

        // Apply enhanced styling
        chart = enhanceChart(chart, "Consumer Lag by Topics for Cluster: " + clusterName, yAxisLabel, true);

        ChartUtils.saveChartAsPNG(new File(outputFileName), chart, 1200, 600);
        logger.info("Saved Kafka lag by topics chart: {} with {} topics", outputFileName, dataset.getSeriesCount());
    }

    public String generateComprehensiveReport(String clusterName,
                                              String dbInstanceIdentifier,
                                              String startTime,
                                              String endTime) throws Exception {
        String timestamp = Instant.now().toString().replaceAll("[:.]+", "-");
        String reportDir = "comprehensive_report_" + timestamp;
        new File(reportDir).mkdirs();

        List<String> generatedCharts = new ArrayList<>();
        String reportSummary = "Comprehensive CloudWatch Report\n";
        reportSummary += "Generated at: " + Instant.now() + "\n";
        reportSummary += "Time Range: " + startTime + " to " + endTime + "\n\n";

        // Generate RDS Charts if dbInstanceIdentifier is provided
        if (dbInstanceIdentifier != null && !dbInstanceIdentifier.trim().isEmpty()) {
            reportSummary += "=== RDS METRICS ===\n";
            reportSummary += "Database Instance: " + dbInstanceIdentifier + "\n\n";

            try {
                // CPU Utilization
                String cpuChart = reportDir + "/rds_cpu_utilization.png";
                plotAndSaveMetric(dbInstanceIdentifier, "CPUUtilization", startTime, endTime, "CPU Utilization (%)", cpuChart);
                generatedCharts.add(cpuChart);
                reportSummary += "✓ CPU Utilization chart generated\n";

                // Freeable Memory
                String memoryChart = reportDir + "/rds_freeable_memory.png";
                plotAndSaveMetric(dbInstanceIdentifier, "FreeableMemory", startTime, endTime, "Freeable Memory (MB)", memoryChart);
                generatedCharts.add(memoryChart);
                reportSummary += "✓ Freeable Memory chart generated\n";

                // Database Connections
                String connectionsChart = reportDir + "/rds_database_connections.png";
                plotAndSaveMetric(dbInstanceIdentifier, "DatabaseConnections", startTime, endTime, "Database Connections (Count)", connectionsChart);
                generatedCharts.add(connectionsChart);
                reportSummary += "✓ Database Connections chart generated\n";

                // Read IOPS
                String readIOPSChart = reportDir + "/rds_read_iops.png";
                plotAndSaveMetric(dbInstanceIdentifier, "ReadIOPS", startTime, endTime, "Read IOPS (Operations/Second)", readIOPSChart);
                generatedCharts.add(readIOPSChart);
                reportSummary += "✓ Read IOPS chart generated\n";

                // Write IOPS
                String writeIOPSChart = reportDir + "/rds_write_iops.png";
                plotAndSaveMetric(dbInstanceIdentifier, "WriteIOPS", startTime, endTime, "Write IOPS (Operations/Second)", writeIOPSChart);
                generatedCharts.add(writeIOPSChart);
                reportSummary += "✓ Write IOPS chart generated\n";

            } catch (Exception e) {
                reportSummary += "✗ Error generating RDS charts: " + e.getMessage() + "\n";
                logger.error("Error generating RDS charts", e);
            }
        }

        // Generate Kafka Charts if clusterName is provided
        if (clusterName != null && !clusterName.trim().isEmpty()) {
            reportSummary += "\n=== KAFKA METRICS ===\n";
            reportSummary += "Cluster Name: " + clusterName + "\n\n";

            // CPU Usage by Broker
            try {
                String cpuBrokerChart = reportDir + "/kafka_cpu_usage_by_broker.png";
                plotAndSaveKafkaBrokerMetric(clusterName, "CpuSystem", startTime, endTime, "CPU Usage (%)", cpuBrokerChart);
                generatedCharts.add(cpuBrokerChart);
                reportSummary += "✓ CPU Usage by Broker chart generated\n";
            } catch (Exception e) {
                reportSummary += "✗ Error generating CPU Usage by Broker chart: " + e.getMessage() + "\n";
                logger.error("Error generating CPU Usage by Broker chart", e);
            }

            // Kafka NFT Dashboard Lag
            try {
                String kafkaLagChart = reportDir + "/kafka_nft_dashboard_lag.png";
                plotAndSaveKafkaNFTDashboardLag(clusterName, startTime, endTime, kafkaLagChart);
                generatedCharts.add(kafkaLagChart);
                reportSummary += "✓ Kafka NFT Dashboard Lag chart generated\n";
            } catch (Exception e) {
                reportSummary += "✗ Error generating Kafka NFT Dashboard Lag chart: " + e.getMessage() + "\n";
                logger.error("Error generating Kafka NFT Dashboard Lag chart", e);
            }

            // Kafka Lag (SumOffsetLag and MaxOffsetLag)
            try {
                String kafkaLagLimitedChart = reportDir + "/kafka_lag_limited.png";
                plotAndSaveKafkaLagLimited(clusterName, startTime, endTime, kafkaLagLimitedChart);
                generatedCharts.add(kafkaLagLimitedChart);
                reportSummary += "✓ Kafka Lag (SumOffsetLag and MaxOffsetLag) chart generated\n";
            } catch (Exception e) {
                reportSummary += "✗ Error generating Kafka Lag chart: " + e.getMessage() + "\n";
                logger.error("Error generating Kafka Lag chart", e);
            }

            // Kafka Time Lag (Time-based lag in milliseconds)
            try {
                String kafkaTimeLagChart = reportDir + "/kafka_time_lag.png";
                plotAndSaveKafkaTimeLagLimited(clusterName, startTime, endTime, kafkaTimeLagChart);
                generatedCharts.add(kafkaTimeLagChart);
                reportSummary += "✓ Kafka Time Lag (Time-based lag in milliseconds) chart generated\n";
            } catch (Exception e) {
                reportSummary += "✗ Error generating Kafka Time Lag chart: " + e.getMessage() + "\n";
                logger.error("Error generating Kafka Time Lag chart", e);
            }
        }

        // Generate summary file
        String summaryFile = reportDir + "/report_summary.txt";
        try (java.io.FileWriter writer = new java.io.FileWriter(summaryFile)) {
            writer.write(reportSummary);
            writer.write("\nGenerated Charts:\n");
            for (String chart : generatedCharts) {
                writer.write("- " + chart + "\n");
            }
        }

        logger.info("Comprehensive report generated in directory: {}", reportDir);
        logger.info("Generated {} charts", generatedCharts.size());

        // Create structured data file for AI analysis
        createStructuredMetricsData(reportDir, clusterName, dbInstanceIdentifier, startTime, endTime);

        return new File(reportDir).getAbsolutePath();
    }

    public String generatePDFReport(String clusterName,
                                    String dbInstanceIdentifier,
                                    String startTime,
                                    String endTime) throws Exception {
        // First generate the comprehensive report with charts
        String reportDir = generateComprehensiveReport(clusterName, dbInstanceIdentifier, startTime, endTime);

        // Create PDF file name
        String timestamp = Instant.now().toString().replaceAll("[:.]+", "-");
        String pdfFileName = "comprehensive_report_" + timestamp + ".pdf";

        // Create PDF document
        Document document = new Document(PageSize.A4, 36, 36, 54, 54);
        PdfWriter.getInstance(document, new FileOutputStream(pdfFileName));
        document.open();

        // Add title
        com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 18, com.itextpdf.text.Font.BOLD);
        Paragraph title = new Paragraph("CloudWatch Comprehensive Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // Add metadata
        com.itextpdf.text.Font metaFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 12, com.itextpdf.text.Font.NORMAL);
        Paragraph metadata = new Paragraph();
        metadata.add(new Chunk("Generated at: " + Instant.now() + "\n", metaFont));
        metadata.add(new Chunk("Time Range: " + startTime + " to " + endTime + "\n", metaFont));
        if (clusterName != null && !clusterName.trim().isEmpty()) {
            metadata.add(new Chunk("Kafka Cluster: " + clusterName + "\n", metaFont));
        }
        if (dbInstanceIdentifier != null && !dbInstanceIdentifier.trim().isEmpty()) {
            metadata.add(new Chunk("RDS Instance: " + dbInstanceIdentifier + "\n", metaFont));
        }
        metadata.setSpacingAfter(20);
        document.add(metadata);

        // Read and add summary content
        String summaryFile = reportDir + "/report_summary.txt";
        try {
            java.nio.file.Path summaryPath = java.nio.file.Paths.get(summaryFile);
            String summaryContent = java.nio.file.Files.readString(summaryPath);

            com.itextpdf.text.Font summaryFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.COURIER, 10, com.itextpdf.text.Font.NORMAL);
            Paragraph summaryParagraph = new Paragraph(summaryContent, summaryFont);
            summaryParagraph.setSpacingAfter(30);
            document.add(summaryParagraph);
        } catch (Exception e) {
            logger.warn("Could not read summary file: {}", e.getMessage());
        }

        // Add charts with headings
        com.itextpdf.text.Font headingFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 14, com.itextpdf.text.Font.BOLD);

        // RDS Charts Section
        if (dbInstanceIdentifier != null && !dbInstanceIdentifier.trim().isEmpty()) {
            // RDS Section Header
            Paragraph rdsHeader = new Paragraph("RDS Database Metrics", headingFont);
            rdsHeader.setSpacingBefore(20);
            rdsHeader.setSpacingAfter(15);
            document.add(rdsHeader);

            // Add RDS charts
            addChartToPDF(document, reportDir + "/rds_cpu_utilization.png", "CPU Utilization");
            addChartToPDF(document, reportDir + "/rds_freeable_memory.png", "Freeable Memory");
            addChartToPDF(document, reportDir + "/rds_database_connections.png", "Database Connections");
            addChartToPDF(document, reportDir + "/rds_read_iops.png", "Read IOPS");
            addChartToPDF(document, reportDir + "/rds_write_iops.png", "Write IOPS");
        }

        // Kafka Charts Section
        if (clusterName != null && !clusterName.trim().isEmpty()) {
            // Kafka Section Header
            Paragraph kafkaHeader = new Paragraph("Kafka Cluster Metrics", headingFont);
            kafkaHeader.setSpacingBefore(20);
            kafkaHeader.setSpacingAfter(15);
            document.add(kafkaHeader);

            // Add Kafka charts
            addChartToPDF(document, reportDir + "/kafka_consumer_lag_all_groups.png", "Consumer Lag (All Groups)");
            addChartToPDF(document, reportDir + "/kafka_cpu_usage_by_broker.png", "CPU Usage by Broker");
            addChartToPDF(document, reportDir + "/kafka_nft_dashboard_lag.png", "Kafka NFT Dashboard Lag");
            addChartToPDF(document, reportDir + "/kafka_lag_limited.png", "Kafka Lag (SumOffsetLag and MaxOffsetLag)");
            addChartToPDF(document, reportDir + "/kafka_time_lag.png", "Kafka Time Lag (Time-based lag in milliseconds)");
        }

        document.close();

        logger.info("PDF report generated: {}", pdfFileName);
        return new File(pdfFileName).getAbsolutePath();
    }

    public void plotAndSaveKafkaMultiMetricLag(String clusterName,
                                               String consumerGroup,
                                               String topic,
                                               String startTimeStr,
                                               String endTimeStr,
                                               String outputFileName) throws Exception {
        Instant startTime = Instant.parse(startTimeStr);
        Instant endTime = Instant.parse(endTimeStr);

        TimeSeriesCollection dataset = new TimeSeriesCollection();

        // Define the lag metrics to plot
        String[] lagMetrics = {"SumOffsetLag", "MaxOffsetLag", "RollingEstimatedTimeLagMax", "EstimatedMaxTimeLag"};

        for (String metricName : lagMetrics) {
            List<Dimension> dimensions = new ArrayList<>();
            dimensions.add(Dimension.builder()
                    .name("Cluster Name")
                    .value(clusterName)
                    .build());
            dimensions.add(Dimension.builder()
                    .name("Consumer Group")
                    .value(consumerGroup)
                    .build());
            dimensions.add(Dimension.builder()
                    .name("Topic")
                    .value(topic)
                    .build());

            GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                    .namespace("AWS/Kafka")
                    .metricName(metricName)
                    .dimensions(dimensions)
                    .startTime(startTime)
                    .endTime(endTime)
                    .period(60) // 1 minute
                    .statistics(Statistic.MAXIMUM, Statistic.AVERAGE)
                    .build();

            try {
                GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(request);

                List<Datapoint> datapoints = new ArrayList<>(response.datapoints());
                if (!datapoints.isEmpty()) {
                    datapoints.sort((a, b) -> a.timestamp().compareTo(b.timestamp()));

                    TimeSeries series = new TimeSeries(metricName);

                    for (Datapoint dp : datapoints) {
                        double value = dp.average() != null ? dp.average() : 0.0;
                        ZonedDateTime zdt = dp.timestamp().atZone(ZoneId.of("UTC"));
                        series.addOrUpdate(new Millisecond(java.util.Date.from(zdt.toInstant())), value);
                    }

                    dataset.addSeries(series);
                }
            } catch (Exception e) {
                logger.error("Failed to fetch data for metric: {}", metricName, e);
            }
        }

        if (dataset.getSeriesCount() == 0) {
            throw new RuntimeException("No data found for any lag metrics for consumer group: " + consumerGroup + ", topic: " + topic);
        }

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Kafka Lag Metrics for " + consumerGroup + " on " + topic,
                "Time",
                "Lag Value (Mixed Units)",
                dataset,
                true, // Show legend to distinguish metrics
                false,
                false
        );

        // Apply enhanced styling
        chart = enhanceChart(chart, "Kafka Lag Metrics for " + consumerGroup + " on " + topic, "Lag Value (Mixed Units)", true);

        ChartUtils.saveChartAsPNG(new File(outputFileName), chart, 1200, 600);
        logger.info("Saved Kafka multi-metric lag chart: {} with {} metrics", outputFileName, dataset.getSeriesCount());
    }

    public void plotAndSaveKafkaLagLimited(String clusterName,
                                           String startTimeStr,
                                           String endTimeStr,
                                           String outputFileName) throws Exception {
        Instant startTime = Instant.parse(startTimeStr);
        Instant endTime = Instant.parse(endTimeStr);

        // Define the consumer groups and topics (sanitized placeholders)
        String[][] consumerGroupTopicPairs = DEFAULT_CONSUMER_GROUP_TOPIC_PAIRS;

        // Define only the two requested metrics
        String[] metrics = {"SumOffsetLag", "MaxOffsetLag"};

        List<String> generatedCharts = new ArrayList<>();

        // Create separate chart for each consumer group
        for (String[] pair : consumerGroupTopicPairs) {
            String consumerGroup = pair[0];
            String topic = pair[1];
            String shortName = getShortConsumerGroupName(consumerGroup);

            TimeSeriesCollection dataset = new TimeSeriesCollection();

            for (String metricName : metrics) {
                List<Dimension> dimensions = new ArrayList<>();
                dimensions.add(Dimension.builder()
                        .name("Cluster Name")
                        .value(clusterName)
                        .build());
                dimensions.add(Dimension.builder()
                        .name("Consumer Group")
                        .value(consumerGroup)
                        .build());
                dimensions.add(Dimension.builder()
                        .name("Topic")
                        .value(topic)
                        .build());

                GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                        .namespace("AWS/Kafka")
                        .metricName(metricName)
                        .dimensions(dimensions)
                        .startTime(startTime)
                        .endTime(endTime)
                        .period(60) // 1 minute
                        .statistics(Statistic.MAXIMUM, Statistic.AVERAGE)
                        .build();

                try {
                    GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(request);

                    List<Datapoint> datapoints = new ArrayList<>(response.datapoints());
                    if (!datapoints.isEmpty()) {
                        datapoints.sort((a, b) -> a.timestamp().compareTo(b.timestamp()));

                        TimeSeries series = new TimeSeries(metricName);

                        for (Datapoint dp : datapoints) {
                            double value = dp.average() != null ? dp.average() : 0.0;
                            ZonedDateTime zdt = dp.timestamp().atZone(ZoneId.of("UTC"));
                            series.addOrUpdate(new Millisecond(java.util.Date.from(zdt.toInstant())), value);
                        }

                        dataset.addSeries(series);
                    }
                } catch (Exception e) {
                    logger.error("Failed to fetch data for consumer group: {}, topic: {}, metric: {} , error: {}",
                            consumerGroup, topic, metricName, e.getMessage(), e);
                }
            }

            // Create chart for this consumer group if data exists
            if (dataset.getSeriesCount() > 0) {
                JFreeChart chart = ChartFactory.createTimeSeriesChart(
                        "Kafka Lag Metrics for " + shortName + " Consumer Group",
                        "Time",
                        "Lag Value (Messages)",
                        dataset,
                        true, // Show legend to distinguish metrics
                        false,
                        false
                );

                // Apply enhanced styling
                chart = enhanceChart(chart, "Kafka Lag Metrics for " + shortName + " Consumer Group", "Lag Value (Messages)", true);

                // Generate filename for each consumer group
                String baseFileName = outputFileName.replace(".png", "");
                String consumerGroupFileName = baseFileName + "_" + shortName.toLowerCase().replace(" ", "_") + ".png";

                ChartUtils.saveChartAsPNG(new File(consumerGroupFileName), chart, 1200, 600);
                generatedCharts.add(consumerGroupFileName);
                logger.info("Saved Kafka lag chart for {}: {} with {} metrics", shortName, consumerGroupFileName, dataset.getSeriesCount());
            }
        }

        if (generatedCharts.isEmpty()) {
            throw new RuntimeException("No data found for any kafka lag metrics in cluster: " + clusterName);
        }

        logger.info("Generated {} separate Kafka lag charts: {}", generatedCharts.size(), generatedCharts);
    }

    public void plotAndSaveKafkaTimeLagLimited(String clusterName,
                                               String startTimeStr,
                                               String endTimeStr,
                                               String outputFileName) throws Exception {
        Instant startTime = Instant.parse(startTimeStr);
        Instant endTime = Instant.parse(endTimeStr);

        // Define the consumer groups and topics (sanitized placeholders)
        String[][] consumerGroupTopicPairs = DEFAULT_CONSUMER_GROUP_TOPIC_PAIRS;

        // Define only the two time-based lag metrics
        String[] metrics = {"RollingEstimatedTimeLagMax", "EstimatedMaxTimeLag"};

        List<String> generatedCharts = new ArrayList<>();

        // Create separate chart for each consumer group
        for (String[] pair : consumerGroupTopicPairs) {
            String consumerGroup = pair[0];
            String topic = pair[1];
            String shortName = getShortConsumerGroupName(consumerGroup);

            TimeSeriesCollection dataset = new TimeSeriesCollection();

            for (String metricName : metrics) {
                List<Dimension> dimensions = new ArrayList<>();
                dimensions.add(Dimension.builder()
                        .name("Cluster Name")
                        .value(clusterName)
                        .build());
                dimensions.add(Dimension.builder()
                        .name("Consumer Group")
                        .value(consumerGroup)
                        .build());
                dimensions.add(Dimension.builder()
                        .name("Topic")
                        .value(topic)
                        .build());

                GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                        .namespace("AWS/Kafka")
                        .metricName(metricName)
                        .dimensions(dimensions)
                        .startTime(startTime)
                        .endTime(endTime)
                        .period(60) // 1 minute
                        .statistics(Statistic.MAXIMUM, Statistic.AVERAGE)
                        .build();

                try {
                    GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(request);

                    List<Datapoint> datapoints = new ArrayList<>(response.datapoints());
                    if (!datapoints.isEmpty()) {
                        datapoints.sort((a, b) -> a.timestamp().compareTo(b.timestamp()));

                        TimeSeries series = new TimeSeries(metricName);

                        for (Datapoint dp : datapoints) {
                            double value = dp.average() != null ? dp.average() : 0.0;
                            // Keep values in milliseconds as requested
                            ZonedDateTime zdt = dp.timestamp().atZone(ZoneId.of("UTC"));
                            series.addOrUpdate(new Millisecond(java.util.Date.from(zdt.toInstant())), value);
                        }

                        dataset.addSeries(series);
                    }
                } catch (Exception e) {
                    logger.error("Failed to fetch time lag data for consumer group: {}, topic: {}, metric: {}",
                            consumerGroup, topic, metricName, e);
                }
            }

            // Create chart for this consumer group if data exists
            if (dataset.getSeriesCount() > 0) {
                JFreeChart chart = ChartFactory.createTimeSeriesChart(
                        "Kafka Time-based Lag Metrics for " + shortName + " Consumer Group",
                        "Time",
                        "Lag Value (Milliseconds)",
                        dataset,
                        true, // Show legend to distinguish metrics
                        false,
                        false
                );

                // Apply enhanced styling
                chart = enhanceChart(chart, "Kafka Time-based Lag Metrics for " + shortName + " Consumer Group", "Lag Value (Milliseconds)", true);

                // Generate filename for each consumer group
                String baseFileName = outputFileName.replace(".png", "");
                String consumerGroupFileName = baseFileName + "_" + shortName.toLowerCase().replace(" ", "_") + ".png";

                ChartUtils.saveChartAsPNG(new File(consumerGroupFileName), chart, 1200, 600);
                generatedCharts.add(consumerGroupFileName);
                logger.info("Saved Kafka time lag chart for {}: {} with {} metrics", shortName, consumerGroupFileName, dataset.getSeriesCount());
            }
        }

        if (generatedCharts.isEmpty()) {
            throw new RuntimeException("No data found for any kafka time lag metrics in cluster: " + clusterName);
        }

        logger.info("Generated {} separate Kafka time lag charts: {}", generatedCharts.size(), generatedCharts);
    }

    public void plotAndSaveKafkaNFTDashboardLag(String clusterName,
                                                String startTimeStr,
                                                String endTimeStr,
                                                String outputFileName) throws Exception {
        Instant startTime = Instant.parse(startTimeStr);
        Instant endTime = Instant.parse(endTimeStr);

        TimeSeriesCollection dataset = new TimeSeriesCollection();

        // Define the consumer groups and topics (sanitized placeholders)
        String[][] consumerGroupTopicPairs = DEFAULT_CONSUMER_GROUP_TOPIC_PAIRS;

        // Define the metrics to fetch for each consumer group/topic pair
        String[] metrics = {"SumOffsetLag", "MaxOffsetLag", "RollingEstimatedTimeLagMax", "EstimatedMaxTimeLag"};

        for (String[] pair : consumerGroupTopicPairs) {
            String consumerGroup = pair[0];
            String topic = pair[1];

            for (String metricName : metrics) {
                List<Dimension> dimensions = new ArrayList<>();
                dimensions.add(Dimension.builder()
                        .name("Cluster Name")
                        .value(clusterName)
                        .build());
                dimensions.add(Dimension.builder()
                        .name("Consumer Group")
                        .value(consumerGroup)
                        .build());
                dimensions.add(Dimension.builder()
                        .name("Topic")
                        .value(topic)
                        .build());

                GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                        .namespace("AWS/Kafka")
                        .metricName(metricName)
                        .dimensions(dimensions)
                        .startTime(startTime)
                        .endTime(endTime)
                        .period(60) // 1 minute
                        .statistics(Statistic.MAXIMUM, Statistic.AVERAGE)
                        .build();

                try {
                    GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(request);

                    List<Datapoint> datapoints = new ArrayList<>(response.datapoints());
                    if (!datapoints.isEmpty()) {
                        datapoints.sort((a, b) -> a.timestamp().compareTo(b.timestamp()));

                        // Create series name with consumer group and metric for clarity
                        String seriesName = getShortConsumerGroupName(consumerGroup) + " - " + metricName;
                        TimeSeries series = new TimeSeries(seriesName);

                        for (Datapoint dp : datapoints) {
                            double value = dp.average() != null ? dp.average() : 0.0;
                            ZonedDateTime zdt = dp.timestamp().atZone(ZoneId.of("UTC"));
                            series.addOrUpdate(new Millisecond(java.util.Date.from(zdt.toInstant())), value);
                        }

                        dataset.addSeries(series);
                    }
                } catch (Exception e) {
                    // convert this to logger
                    logger.error("Failed to fetch data for consumer group: {}, topic: {}, metric: {} , error: {}",
                            consumerGroup, topic, metricName, e.getMessage(), e);
                }
            }
        }

        if (dataset.getSeriesCount() == 0) {
            throw new RuntimeException("No data found for any NFT dashboard metrics in cluster: " + clusterName);
        }

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Kafka NFT Dashboard Lag Metrics for Cluster: " + clusterName,
                "Time",
                "Lag Value (Mixed Units)",
                dataset,
                true, // Show legend to distinguish series
                false,
                false
        );

        // Apply enhanced styling
        chart = enhanceChart(chart, "Kafka NFT Dashboard Lag Metrics for Cluster: " + clusterName, "Lag Value (Mixed Units)", true);

        ChartUtils.saveChartAsPNG(new File(outputFileName), chart, 1200, 600);
        logger.info("Saved Kafka NFT dashboard lag chart: {} with {} series", outputFileName, dataset.getSeriesCount());
    }

    private String getShortConsumerGroupName(String consumerGroup) {
        String cg = consumerGroup.toLowerCase();
        if (cg.contains("worker")) {
            return "Worker";
        } else if (cg.contains("async")) {
            return "Async Notify";
        } else if (cg.contains("notify")) {
            return "Notify";
        } else {
            return consumerGroup;
        }
    }

    private void addChartToPDF(Document document, String chartPath, String chartTitle) {
        try {
            File chartFile = new File(chartPath);
            if (chartFile.exists()) {
                // Add chart title
                com.itextpdf.text.Font chartTitleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 12, com.itextpdf.text.Font.BOLD);
                Paragraph chartTitleParagraph = new Paragraph(chartTitle, chartTitleFont);
                chartTitleParagraph.setSpacingBefore(15);
                chartTitleParagraph.setSpacingAfter(10);
                document.add(chartTitleParagraph);

                // Add chart image
                com.itextpdf.text.Image chartImage = com.itextpdf.text.Image.getInstance(chartPath);

                // Scale image to fit page width while maintaining aspect ratio
                float pageWidth = document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin();
                float imageWidth = chartImage.getWidth();
                float imageHeight = chartImage.getHeight();

                if (imageWidth > pageWidth) {
                    float scaleFactor = pageWidth / imageWidth;
                    chartImage.scaleToFit(pageWidth, imageHeight * scaleFactor);
                }

                chartImage.setAlignment(Element.ALIGN_CENTER);
                chartImage.setSpacingAfter(20);
                document.add(chartImage);

                // Add page break after each chart except the last one
                document.add(new Paragraph("\n"));
            } else {
                logger.warn("Chart file not found: {}", chartPath);
            }
        } catch (Exception e) {
            logger.error("Error adding chart to PDF: {}", chartPath, e);
        }
    }

    /**
     * Creates structured metrics data in JSON-like format that AI can easily analyze
     */
    private void createStructuredMetricsData(String reportDir, String clusterName, String dbInstanceIdentifier,
                                             String startTime, String endTime) {
        try {
            StringBuilder jsonData = new StringBuilder();
            jsonData.append("{\n");
            jsonData.append("  \"report_metadata\": {\n");
            jsonData.append("    \"generated_at\": \"").append(Instant.now()).append("\",\n");
            jsonData.append("    \"time_range\": {\n");
            jsonData.append("      \"start\": \"").append(startTime).append("\",\n");
            jsonData.append("      \"end\": \"").append(endTime).append("\"\n");
            jsonData.append("    }\n");
            jsonData.append("  },\n");

            Instant startInstant = Instant.parse(startTime);
            Instant endInstant = Instant.parse(endTime);

            // RDS Metrics
            if (dbInstanceIdentifier != null && !dbInstanceIdentifier.trim().isEmpty()) {
                jsonData.append("  \"rds_metrics\": {\n");
                jsonData.append("    \"db_instance\": \"").append(dbInstanceIdentifier).append("\",\n");

                // CPU Utilization
                jsonData.append("    \"cpu_utilization\": {\n");
                jsonData.append("      \"expected_range\": \"40% - 50%\",\n");
                jsonData.append("      \"unit\": \"percent\",\n");
                jsonData.append("      \"data_points\": [\n");
                appendMetricDataPoints(jsonData, "AWS/RDS", "CPUUtilization",
                        List.of(Dimension.builder().name("DBInstanceIdentifier").value(dbInstanceIdentifier).build()),
                        startInstant, endInstant);
                jsonData.append("      ]\n");
                jsonData.append("    },\n");

                // Database Connections
                jsonData.append("    \"database_connections\": {\n");
                jsonData.append("      \"expected_range\": \"2000 - 2500 connections\",\n");
                jsonData.append("      \"unit\": \"connections\",\n");
                jsonData.append("      \"data_points\": [\n");
                appendMetricDataPoints(jsonData, "AWS/RDS", "DatabaseConnections",
                        List.of(Dimension.builder().name("DBInstanceIdentifier").value(dbInstanceIdentifier).build()),
                        startInstant, endInstant);
                jsonData.append("      ]\n");
                jsonData.append("    },\n");

                // Freeable Memory
                jsonData.append("    \"freeable_memory\": {\n");
                jsonData.append("      \"unit\": \"bytes\",\n");
                jsonData.append("      \"data_points\": [\n");
                appendMetricDataPoints(jsonData, "AWS/RDS", "FreeableMemory",
                        List.of(Dimension.builder().name("DBInstanceIdentifier").value(dbInstanceIdentifier).build()),
                        startInstant, endInstant);
                jsonData.append("      ]\n");
                jsonData.append("    }\n");
                jsonData.append("  }");
            }

            // Kafka Metrics
            if (clusterName != null && !clusterName.trim().isEmpty()) {
                if (dbInstanceIdentifier != null && !dbInstanceIdentifier.trim().isEmpty()) {
                    jsonData.append(",\n");
                }
                jsonData.append("  \"kafka_metrics\": {\n");
                jsonData.append("    \"cluster_name\": \"").append(clusterName).append("\",\n");
                jsonData.append("    \"consumer_groups\": {\n");

                String[][] consumerGroupTopicPairs = DEFAULT_CONSUMER_GROUP_TOPIC_PAIRS;

                for (int i = 0; i < consumerGroupTopicPairs.length; i++) {
                    String[] pair = consumerGroupTopicPairs[i];
                    String consumerGroup = pair[0];
                    String topic = pair[1];
                    String shortName = consumerGroup.contains("worker") ? "worker" :
                            consumerGroup.contains("async") ? "async_notify" : "notify";

                    jsonData.append("      \"").append(shortName).append("\": {\n");
                    jsonData.append("        \"consumer_group\": \"").append(consumerGroup).append("\",\n");
                    jsonData.append("        \"topic\": \"").append(topic).append("\",\n");

                    // SumOffsetLag
                    jsonData.append("        \"sum_offset_lag\": {\n");
                    jsonData.append("          \"unit\": \"messages\",\n");
                    jsonData.append("          \"data_points\": [\n");
                    appendMetricDataPoints(jsonData, "AWS/Kafka", "SumOffsetLag",
                            List.of(
                                    Dimension.builder().name("Cluster Name").value(clusterName).build(),
                                    Dimension.builder().name("Consumer Group").value(consumerGroup).build(),
                                    Dimension.builder().name("Topic").value(topic).build()
                            ), startInstant, endInstant);
                    jsonData.append("          ]\n");
                    jsonData.append("        },\n");

                    // MaxOffsetLag
                    jsonData.append("        \"max_offset_lag\": {\n");
                    jsonData.append("          \"unit\": \"messages\",\n");
                    jsonData.append("          \"data_points\": [\n");
                    appendMetricDataPoints(jsonData, "AWS/Kafka", "MaxOffsetLag",
                            List.of(
                                    Dimension.builder().name("Cluster Name").value(clusterName).build(),
                                    Dimension.builder().name("Consumer Group").value(consumerGroup).build(),
                                    Dimension.builder().name("Topic").value(topic).build()
                            ), startInstant, endInstant);
                    jsonData.append("          ]\n");
                    jsonData.append("        }\n");
                    jsonData.append("      }");
                    if (i < consumerGroupTopicPairs.length - 1) {
                        jsonData.append(",");
                    }
                    jsonData.append("\n");
                }

                jsonData.append("    }\n");
                jsonData.append("  }\n");
            }

            jsonData.append("}\n");

            // Write structured data to file
            String dataFileName = reportDir + "/metrics_vector_data.json";
            java.nio.file.Files.writeString(java.nio.file.Paths.get(dataFileName), jsonData.toString());
            logger.info("Created structured metrics data file for AI analysis: {}", dataFileName);

        } catch (Exception e) {
            logger.error("Error creating structured metrics data file: {}", e.getMessage(), e);
        }
    }

    /**
     * Appends metric data points in JSON format
     */
    private void appendMetricDataPoints(StringBuilder jsonData, String namespace, String metricName,
                                        List<Dimension> dimensions, Instant startTime, Instant endTime) {
        try {
            GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                    .namespace(namespace)
                    .metricName(metricName)
                    .dimensions(dimensions)
                    .startTime(startTime)
                    .endTime(endTime)
                    .period(300) // 5 minute intervals
                    .statistics(Statistic.AVERAGE, Statistic.MAXIMUM, Statistic.MINIMUM)
                    .build();

            GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(request);
            List<Datapoint> datapoints = new ArrayList<>(response.datapoints());

            if (!datapoints.isEmpty()) {
                datapoints.sort((a, b) -> a.timestamp().compareTo(b.timestamp()));

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                dateFormat.setTimeZone(java.util.TimeZone.getTimeZone(chartTimezone));

                for (int i = 0; i < datapoints.size(); i++) {
                    Datapoint dp = datapoints.get(i);
                    String timestamp = dateFormat.format(java.util.Date.from(dp.timestamp()));
                    double avg = dp.average() != null ? dp.average() : 0.0;
                    double max = dp.maximum() != null ? dp.maximum() : 0.0;
                    double min = dp.minimum() != null ? dp.minimum() : 0.0;

                    jsonData.append("        {\n");
                    jsonData.append("          \"timestamp\": \"").append(timestamp).append("\",\n");
                    jsonData.append("          \"average\": ").append(String.format("%.2f", avg)).append(",\n");
                    jsonData.append("          \"maximum\": ").append(String.format("%.2f", max)).append(",\n");
                    jsonData.append("          \"minimum\": ").append(String.format("%.2f", min)).append("\n");
                    jsonData.append("        }");
                    if (i < datapoints.size() - 1) {
                        jsonData.append(",");
                    }
                    jsonData.append("\n");
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching metric data points for {}: {}", metricName, e.getMessage(), e);
        }
    }

}
