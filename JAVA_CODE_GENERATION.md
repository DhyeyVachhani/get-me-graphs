# AI Java Code Generation and Automatic Execution

## Overview

The stability analysis has been enhanced to generate **Java code** instead of Python code, automatically compile it, and execute it to generate visualization charts. This provides seamless integration with the existing Java ecosystem and JFreeChart library.

## Enhanced Functionality

### 1. Java Code Generation
The AI now generates complete Java programs that:
- Use **JFreeChart** for professional chart generation
- Parse JSON vector data using **Jackson**
- Create time-series charts with correlation analysis
- Save charts as PNG files automatically
- Include proper error handling and logging

### 2. Automatic Compilation and Execution
When Java code is detected, the system:
- **Extracts** the Java code from AI response
- **Creates** a complete `.java` source file
- **Compiles** using `javac` with proper classpath
- **Executes** the compiled program
- **Generates** charts directly to the report directory

### 3. Generated Files

#### Source Files
- **`StabilityAnalysisVisualization.java`**: Complete Java source code
- **`StabilityAnalysisVisualization.class`**: Compiled bytecode
- **`metrics_data.json`**: Clean JSON data

#### Generated Charts
- **`cpu_correlation_chart.png`**: CPU vs time with anomaly markers
- **`memory_trends_chart.png`**: Memory usage patterns
- **`kafka_lag_analysis.png`**: Consumer lag correlation
- **`stability_overview.png`**: Combined stability metrics

## AI Prompt Enhancement

The AI receives this enhanced instruction:

```
6. **Java Visualization Program:**
   - Generate a complete Java program using JFreeChart
   - Include code to parse JSON data and create time-series charts
   - Add markers for values outside expected ranges
   - Make the program ready to compile and run
   - Use JFreeChart library for creating professional charts
   - Save the generated charts as PNG files
```

## Generated Java Code Structure

```java
// AI-Generated Stability Analysis Visualization Program
// Generated from CloudWatch metrics vector data

import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.data.time.*;
import org.jfree.data.xy.*;
import com.fasterxml.jackson.databind.*;
import java.io.*;
import java.text.*;
import java.util.*;

public class StabilityAnalysisVisualization {
    
    // Vector data from CloudWatch
    private static final String VECTOR_DATA = """
    {
      "report_metadata": {...},
      "rds_metrics": {...},
      "kafka_metrics": {...}
    }
    """;

    public static void main(String[] args) throws Exception {
        // AI-generated visualization code
        ObjectMapper mapper = new ObjectMapper();
        JsonNode data = mapper.readTree(VECTOR_DATA);
        
        // Create CPU utilization chart
        createCpuChart(data);
        
        // Create memory trends chart
        createMemoryChart(data);
        
        // Create Kafka lag analysis
        createKafkaChart(data);
        
        // Create stability overview
        createStabilityOverview(data);
    }
    
    // AI-generated chart creation methods...
}
```

## Execution Flow

### 1. AI Analysis Request
```bash
curl -X POST "http://localhost:8080/analysis/stability" \
  -d "clusterName=kafka-cluster" \
  -d "dbInstanceIdentifier=prod-db" \
  -d "startTime=2025-09-10T10:00:00.000Z" \
  -d "endTime=2025-09-10T14:00:00.000Z"
```

### 2. Code Generation Process
1. **AI analyzes** vector data and generates Java code
2. **System extracts** Java code from AI response
3. **Creates** complete Java source file with embedded data
4. **Compiles** using current classpath (includes JFreeChart)
5. **Executes** compiled program
6. **Charts generated** automatically in report directory

### 3. Response
```
Report Directory: comprehensive_report_2025-09-11T10-46-37-332201900Z
Vector Data File: comprehensive_report_2025-09-11T10-46-37-332201900Z/metrics_vector_data.json
Generated Files: Check directory for Java visualization program, compiled charts, and JSON data

=== STABILITY ANALYSIS (Based on Vector Data) ===
[Detailed analysis with insights]

[Charts automatically generated and saved]
```

## Benefits

### 1. **Native Integration**
- Uses existing JFreeChart dependency
- No external Python/matplotlib requirements
- Consistent with application architecture
- Same styling and theming as existing charts

### 2. **Automatic Execution**
- No manual compilation or execution steps
- Charts generated immediately
- Ready-to-view PNG files
- Seamless user experience

### 3. **Professional Charts**
- JFreeChart quality and styling
- Consistent with existing application charts
- Professional annotations and legends
- High-resolution output

### 4. **Error Handling**
- Compilation errors are logged and reported
- Runtime errors are caught and handled
- Graceful fallback if code generation fails
- Detailed error messages for debugging

## Technical Implementation

### Compilation Process
```java
ProcessBuilder compileBuilder = new ProcessBuilder(
    "javac", 
    "-cp", classpath,
    javaFileName
);
```

### Execution Process
```java
ProcessBuilder runBuilder = new ProcessBuilder(
    "java",
    "-cp", classpath + File.pathSeparator + reportDir,
    "StabilityAnalysisVisualization"
);
```

### Classpath Management
- Uses current application classpath
- Includes JFreeChart and Jackson libraries
- Adds report directory for execution

## Generated Chart Types

### 1. **CPU Utilization Analysis**
- Time-series plot of CPU percentage
- Red markers for values above 50%
- Blue markers for values below 40%
- Trend lines and statistical overlays

### 2. **Memory Trends**
- Freeable memory over time
- Memory pressure indicators
- Leak detection patterns
- Capacity planning insights

### 3. **Kafka Lag Correlation**
- Consumer group lag comparison
- Spike correlation analysis
- Topic-specific patterns
- Processing bottleneck identification

### 4. **Stability Overview**
- Multi-metric correlation chart
- Stability score visualization
- Anomaly period highlighting
- Overall system health indicators

## Error Scenarios

### Compilation Failures
- Missing dependencies reported
- Syntax errors in generated code
- Classpath issues identified
- Detailed error output provided

### Runtime Failures
- Data parsing errors handled
- Chart generation failures logged
- File I/O issues reported
- Graceful degradation

## File Outputs

After successful execution, the report directory contains:
- **Source files**: `.java` and `.class` files
- **Data files**: JSON data in multiple formats
- **Chart files**: High-quality PNG images
- **Log files**: Execution details and any errors

This enhancement provides a seamless, integrated solution for AI-generated chart visualization that runs natively within the Java application environment.
