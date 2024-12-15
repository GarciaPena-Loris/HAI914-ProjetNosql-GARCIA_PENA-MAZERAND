package qengine.benchmark;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.chart.plot.CategoryPlot;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BenchmarkResultsAggregator {

    public static void main(String[] args) throws IOException {
        String directoryPath = "benchmark";

        System.out.println("Starting aggregation of results from directory: " + directoryPath);
        List<BenchmarkResultsAggregator.BenchmarkResult> results = BenchmarkResultsAggregator.aggregateResults(directoryPath);
        System.out.println("Aggregation complete. Generating chart...");
        generateChart(results, "benchmark/benchmark_results_chart.png");
        System.out.println("Chart generated successfully.");
    }

    public static class BenchmarkResult {
        public String system;
        public double averageExecutionTime;

        public BenchmarkResult(String system, double averageExecutionTime) {
            this.system = system;
            this.averageExecutionTime = averageExecutionTime;
        }
    }

    public static List<BenchmarkResult> aggregateResults(String directoryPath) throws IOException {
        List<BenchmarkResult> results = new ArrayList<>();
        List<String> filePaths = findResultFiles(directoryPath);

        System.out.println("Found " + filePaths.size() + " result files.");
        for (String filePath : filePaths) {
            System.out.println("Processing file: " + filePath);
            String system = categorizeSystem(filePath);
            if (system == null) {
                continue;
            }
            double averageExecutionTime = calculateAverageExecutionTime(filePath);
            System.out.println("System: " + system + ", Average Execution Time: " + averageExecutionTime + " ms");
            results.add(new BenchmarkResult(system, averageExecutionTime));
        }

        return results;
    }

    private static List<String> findResultFiles(String directoryPath) {
        List<String> resultFiles = new ArrayList<>();
        File directory = new File(directoryPath);

        if (directory.exists() && directory.isDirectory()) {
            findResultFilesRecursively(directory, resultFiles);
        }

        return resultFiles;
    }

    private static void findResultFilesRecursively(File directory, List<String> resultFiles) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    findResultFilesRecursively(file, resultFiles);
                } else if (file.getName().startsWith("combined_results_") && file.getName().endsWith(".csv")) {
                    resultFiles.add(file.getAbsolutePath());
                }
            }
        }
    }

    private static String categorizeSystem(String filePath) {
        String size = "";
        if (filePath.contains("500k")) {
            size = "500k";
        } else if (filePath.contains("1M")) {
            size = "1M";
        } else if (filePath.contains("2M")) {
            size = "2M";
        }

        if (filePath.contains("other")) {
            return "concurrent (" + size + ")";
        } else if (filePath.contains("integral")) {
            return "integral (" + size + ")";
        } else {
            return "Hexastore (" + size + ")";
        }
    }

    private static double calculateAverageExecutionTime(String filePath) throws IOException {
        List<Double> executionTimes = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                executionTimes.add(Double.parseDouble(values[1]));
            }
        }
        return executionTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    public static void generateChart(List<BenchmarkResult> results, String outputFilePath) throws IOException {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        // Sort results by system and then by file size
        results.sort(Comparator.comparing((BenchmarkResult result) -> {
            if (result.system.contains("Hexastore")) return 1;
            if (result.system.contains("integral")) return 2;
            if (result.system.contains("concurrent")) return 3;
            return 4;
        }).thenComparing(result -> {
            if (result.system.contains("500k")) return 1;
            if (result.system.contains("1M")) return 2;
            if (result.system.contains("2M")) return 3;
            return 4;
        }));

        for (BenchmarkResult result : results) {
            System.out.println("Adding data to chart: System = " + result.system + ", Average Execution Time = " + result.averageExecutionTime + " ms");
            dataset.addValue(result.averageExecutionTime, result.system, "Average Execution Time");
        }

        JFreeChart barChart = ChartFactory.createBarChart(
                "Benchmark Results",
                "System",
                "Average Execution Time (ms)",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        // Set colors
        CategoryPlot plot = barChart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        for (int i = 0; i < dataset.getRowCount(); i++) {
            String rowKey = (String) dataset.getRowKey(i);
            if (rowKey.contains("Hexastore")) {
                if (rowKey.contains("500k")) {
                    renderer.setSeriesPaint(i, Color.BLUE);
                } else if (rowKey.contains("1M")) {
                    renderer.setSeriesPaint(i, Color.BLUE.darker());
                } else if (rowKey.contains("2M")) {
                    renderer.setSeriesPaint(i, Color.BLUE.darker().darker());
                }
            } else if (rowKey.contains("integral")) {
                if (rowKey.contains("500k")) {
                    renderer.setSeriesPaint(i, Color.GREEN);
                } else if (rowKey.contains("1M")) {
                    renderer.setSeriesPaint(i, Color.GREEN.darker());
                } else if (rowKey.contains("2M")) {
                    renderer.setSeriesPaint(i, Color.GREEN.darker().darker());
                }
            } else if (rowKey.contains("concurrent")) {
                if (rowKey.contains("500k")) {
                    renderer.setSeriesPaint(i, Color.RED);
                } else if (rowKey.contains("1M")) {
                    renderer.setSeriesPaint(i, Color.RED.darker());
                } else if (rowKey.contains("2M")) {
                    renderer.setSeriesPaint(i, Color.RED.darker().darker());
                }
            }
        }

        // Calculate overall average execution time for each system
        double hexastoreAvg = results.stream().filter(r -> r.system.contains("Hexastore")).mapToDouble(r -> r.averageExecutionTime).average().orElse(0.0);
        double integralAvg = results.stream().filter(r -> r.system.contains("integral")).mapToDouble(r -> r.averageExecutionTime).average().orElse(0.0);
        double concurrentAvg = results.stream().filter(r -> r.system.contains("concurrent")).mapToDouble(r -> r.averageExecutionTime).average().orElse(0.0);

        // Add legend with overall average execution time
        barChart.addSubtitle(new org.jfree.chart.title.TextTitle(
                String.format("Overall Average Execution Time:\nHexastore: %.2f ms\nintegral: %.2f ms\nconcurrent: %.2f ms", hexastoreAvg, integralAvg, concurrentAvg)
        ));

        ChartUtils.saveChartAsPNG(new File(outputFilePath), barChart, 800, 600);
    }
}