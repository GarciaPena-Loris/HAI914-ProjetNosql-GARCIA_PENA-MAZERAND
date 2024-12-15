package qengine.benchmark;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYIntervalSeries;
import org.jfree.data.xy.XYIntervalSeriesCollection;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class HistogramGenerator {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: java HistogramGenerator <input_csv_file_name>");
            System.exit(1);
        }

        String inputCsvFileName = args[0];

        String inputCsvFile = "benchmark/" + inputCsvFileName + "/combined_results_" + inputCsvFileName + ".csv";
        String outputImageFile = "benchmark/" + inputCsvFileName + "/histogram_" + inputCsvFileName + ".png";

        // Lire les temps d'exécution depuis le fichier CSV
        List<Double> executionTimes = readExecutionTimesFromCsv(inputCsvFile);
        List<Integer> numberOfQueries = readNumberOfQueriesFromCsv(inputCsvFile);

        // Convertir en tableau
        double[] data = executionTimes.stream().mapToDouble(Double::doubleValue).toArray();
        int[] queries = numberOfQueries.stream().mapToInt(Integer::intValue).toArray();

        // Générer l'histogramme avec toutes les fonctionnalités
        generateHistogram(data, queries, "Execution Times Histogram for " + inputCsvFileName, "Number of Queries", "Execution Time (ms)", outputImageFile);
    }

    private static List<Double> readExecutionTimesFromCsv(String csvFile) throws IOException {
        List<Double> executionTimes = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            br.readLine(); // Ignorer l'en-tête
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                executionTimes.add(Double.parseDouble(values[1]));
            }
        }
        return executionTimes;
    }

    private static List<Integer> readNumberOfQueriesFromCsv(String csvFile) throws IOException {
        List<Integer> numberOfQueries = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            br.readLine(); // Ignorer l'en-tête
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                numberOfQueries.add(Integer.parseInt(values[0]));
            }
        }
        return numberOfQueries;
    }

    public static void generateHistogram(double[] data, int[] queries, String title, String xAxisLabel, String yAxisLabel, String outputFilePath) throws IOException {
        // Create a dataset for the bars
        IntervalXYDataset dataset = createDataset(data, queries);

        // Calculate the average execution time
        double averageExecutionTime = Math.round(calculateAverage(data)  * 100.0) / 100.0;
        double averageExecutionTimeForOneQuery = Math.round(calculateAverageTimeForOneQuery(data, queries) * 100.0) / 100.0;

        // Create the histogram
        JFreeChart histogram = ChartFactory.createXYBarChart(
                title,
                xAxisLabel,
                false,
                yAxisLabel,
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        // Customize the bar and the chart
        XYPlot plot = (XYPlot) histogram.getPlot();
        ValueMarker avgMarker = getValueMarker(data, queries, plot);
        plot.addRangeMarker(avgMarker);

        // Add the average execution time to the legend
        histogram.addSubtitle(new TextTitle("Average Execution Time: " + averageExecutionTime + " ms" + " (" + averageExecutionTimeForOneQuery + " ms per query)"));

        // Save the chart
        ChartUtils.saveChartAsPNG(new File(outputFilePath), histogram, 800, 600);
    }

    private static ValueMarker getValueMarker(double[] data, int[] queries, XYPlot plot) {
        XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
        renderer.setDrawBarOutline(false);
        renderer.setSeriesPaint(0, new Color(0, 102, 204)); // Bar color

        // Add the red average line
        double averageExecutionTime = calculateAverage(data);
        ValueMarker avgMarker = new ValueMarker(averageExecutionTime);
        avgMarker.setPaint(Color.RED);
        avgMarker.setStroke(new BasicStroke(2.0f));
        avgMarker.setLabel("Average Execution Time");
        avgMarker.setLabelAnchor(RectangleAnchor.TOP_LEFT);
        avgMarker.setLabelTextAnchor(TextAnchor.TOP_LEFT);
        return avgMarker;
    }

    private static IntervalXYDataset createDataset(double[] data, int[] queries) {
        XYIntervalSeries series = new XYIntervalSeries("Execution Times");

        for (int i = 0; i < data.length; i++) {
            double value = data[i];
            double query = queries[i];
            series.add(query, query - 0.5, query + 0.5, value, value, value);
        }

        XYIntervalSeriesCollection dataset = new XYIntervalSeriesCollection();
        dataset.addSeries(series);
        return dataset;
    }

    private static double calculateAverage(double[] data) {
        double sum = 0;
        for (double value : data) {
            sum += value;
        }
        return sum / data.length;
    }

    private static double calculateAverageTimeForOneQuery(double[] data, int[] queries) {
        // Variable pour stocker la somme des temps d'exécution
        double totalExecutionTime = 0;

        // Variable pour stocker le nombre total de requêtes
        int totalQueries = 0;

        // Parcours des tableaux 'data' et 'queries' pour additionner les temps d'exécution et le nombre total de requêtes
        for (int i = 0; i < data.length; i++) {
            totalExecutionTime += data[i];  // Ajoute le temps d'exécution de chaque entrée
            totalQueries += queries[i];     // Ajoute le nombre de requêtes de chaque entrée
        }

        // Calcul et retour du temps moyen par requête
        return totalExecutionTime / totalQueries;
    }

}