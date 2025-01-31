package net.berack.upo.valpre;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import net.berack.upo.valpre.sim.stats.CsvResult;
import net.berack.upo.valpre.sim.stats.ResultSummary;
import net.berack.upo.valpre.sim.stats.Statistics;

/**
 * This class is used to plot the results of the simulation.
 * The results are saved in a CSV file and then loaded to be plotted.
 */
public class Plot {
    public final ResultSummary summary;
    private final ChartPanel chartPanel;
    private final JComboBox<String> nodeComboBox;
    private final JComboBox<String> statComboBox;

    /**
     * Create a new plot object.
     * 
     * @param args the arguments to create the plot
     * @throws IOException if anything happens while reading the file
     */
    public Plot(String[] args) throws IOException {
        var arguments = Plot.parseParameters(args);
        var file = Parameters.getFileOrExample(arguments.get("csv"));
        if (file == null)
            throw new IllegalArgumentException("CSV file needed! Use -csv <file>");

        var results = new CsvResult(file).loadResults();
        this.summary = new ResultSummary(results);

        var nodes = this.summary.getNodes().toArray(new String[0]);
        this.chartPanel = new ChartPanel(null);
        this.nodeComboBox = new JComboBox<>(nodes);
        this.statComboBox = new JComboBox<>(Statistics.getOrderOfApply());
    }

    /**
     * Show the plot of the results.
     */
    public void show() {
        SwingUtilities.invokeLater(() -> {
            var nodeLabel = new JLabel("Node: ");
            var statLabel = new JLabel("Stat: ");

            var filterPanel = new JPanel();
            filterPanel.setLayout(new GridLayout(2, 2));
            filterPanel.add(nodeLabel);
            filterPanel.add(nodeComboBox);
            filterPanel.add(statLabel);
            filterPanel.add(statComboBox);

            nodeComboBox.addActionListener(_ -> updateChart());
            statComboBox.addActionListener(_ -> updateChart());

            var rootPane = new JPanel();
            rootPane.setLayout(new BorderLayout());
            rootPane.add(filterPanel, BorderLayout.NORTH);
            rootPane.add(chartPanel, BorderLayout.CENTER);

            chartPanel.setChart(ChartFactory.createBarChart(
                    "Title",
                    "Run",
                    "Value",
                    null,
                    PlotOrientation.VERTICAL,
                    true,
                    true,
                    false));
            updateChart();

            var frame = new JFrame("Graph of the Simulation");
            frame.add(rootPane);
            frame.setSize(800, 600);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }

    /**
     * Update the chart with the selected node and stat.
     */
    private void updateChart() {
        try {
            var node = this.nodeComboBox.getSelectedItem().toString();
            var stat = this.statComboBox.getSelectedItem().toString();

            var summary = this.summary.getSummaryOf(node, stat);
            var frequency = summary.getFrequency(20);

            var dataset = new DefaultCategoryDataset();
            for (int i = 0; i < frequency.length; i++) {
                dataset.addValue(frequency[i], "Frequency", Integer.valueOf(i));
            }

            var chart = chartPanel.getChart();
            chart.getCategoryPlot().setDataset(dataset);
            chart.setTitle(String.format("Avg %.3f", summary.average));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Parse the arguments to get the CSV file.
     * 
     * @param args the arguments to parse
     * @return a map with the arguments
     */
    private static Map<String, String> parseParameters(String[] args) {
        var arguments = new HashMap<String, Boolean>();
        arguments.put("csv", true);

        var descriptions = new HashMap<String, String>();
        descriptions.put("csv", "The filename that contains the previous saved runs.");

        return Parameters.getArgsOrHelper(args, "-", arguments, descriptions);
    }
}
