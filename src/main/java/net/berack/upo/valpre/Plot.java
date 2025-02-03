package net.berack.upo.valpre;

import java.awt.BorderLayout;
import java.awt.Font;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.CategoryLabelPositions;
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
    private final ChartPanel panelBarChart;
    private final JComboBox<String> nodeComboBox;
    private final JList<JListEntry> statList;

    /**
     * Create a new plot object.
     * 
     * @param args the arguments to create the plot
     * @throws IOException if anything happens while reading the file
     */
    public Plot(String csv) throws IOException {
        var stream = Parameters.getFileOrExample(csv);
        if (stream == null)
            throw new IllegalArgumentException("CSV file needed!");
        var results = CsvResult.loadResults(stream);
        stream.close();

        this.summary = new ResultSummary(results);

        var nodes = this.summary.getNodes().toArray(new String[0]);
        this.panelBarChart = new ChartPanel(null);

        this.nodeComboBox = new JComboBox<>(nodes);
        this.nodeComboBox.addActionListener(_ -> update());

        var order = Statistics.getOrderOfApply();
        var panels = new JListEntry[order.length];
        for (int i = 0; i < order.length; i++)
            panels[i] = new JListEntry(order[i]);

        this.statList = new JList<>(panels);
        this.statList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.statList.addListSelectionListener(_ -> update());
        this.statList.setFixedCellHeight(25);
        this.statList.setCellRenderer((list, val, _, selected, _) -> {
            var bgColor = list.getBackground();
            var bgSelColor = list.getSelectionBackground();
            val.setBackground(selected ? bgSelColor : bgColor);

            return val;
        });
    }

    /**
     * Show the plot of the results.
     * This method creates the GUI and shows the plot of the results.
     * The user can select the node and the statistic to show.
     * The plot is updated when the user selects a different node or statistic.
     * The plot shows the distribution of the runs and the mean and error of the
     * statistic.
     * The plot is shown in a new window.
     */
    public void show() {
        SwingUtilities.invokeLater(() -> {
            // Create charts with empty data
            this.panelBarChart.setChart(ChartFactory.createBarChart(
                    "Run Distributions",
                    "",
                    "",
                    null,
                    PlotOrientation.VERTICAL,
                    false,
                    true,
                    false));
            this.panelBarChart.getChart().getCategoryPlot().getDomainAxis()
                    .setCategoryLabelPositions(CategoryLabelPositions.UP_45);

            // Create the GUI with the various layouts and components
            var filterPanel = new JPanel();
            filterPanel.setLayout(new BorderLayout());
            filterPanel.add(new JLabel("Node: "), BorderLayout.WEST);
            filterPanel.add(this.nodeComboBox, BorderLayout.CENTER);
            filterPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

            var rootPane = new JPanel();
            rootPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            rootPane.setLayout(new BorderLayout());
            rootPane.add(filterPanel, BorderLayout.NORTH);
            rootPane.add(this.statList, BorderLayout.WEST);
            rootPane.add(this.panelBarChart, BorderLayout.CENTER);

            // update the charts by triggering the event
            this.statList.setSelectedIndex(0);

            // Show the frame
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
    private void update() {
        try {
            var node = this.nodeComboBox.getSelectedItem().toString();
            var stat = this.statList.getSelectedValue().name.getText();

            var summary = this.summary.getSummaryOf(node);
            var statSummary = summary.get(stat);
            var frequency = statSummary.getFrequency(15);

            var dataset = new DefaultCategoryDataset();
            var bucket = (statSummary.max - statSummary.min) / frequency.length;
            for (int i = 0; i < frequency.length; i++) {
                var columnVal = statSummary.min + i * bucket;
                var columnKey = String.format("%.3f", columnVal);
                dataset.addValue(frequency[i], "Frequency", columnKey);
            }
            var chart = this.panelBarChart.getChart();
            chart.getCategoryPlot().setDataset(dataset);
            chart.setTitle(stat + " distribution");

            var model = this.statList.getModel();
            for (int i = 0; i < model.getSize(); i++) {
                var entry = model.getElementAt(i);
                var value = summary.get(entry.name.getText());
                entry.value.setText(String.format("%8.3f ±% 9.3f", value.average, value.error95));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This class is used to create a panel with a name and a value.
     * The name is on the left and the value is on the right.
     * The name is in bold and the value is in plain.
     */
    private static class JListEntry extends JPanel {
        public static final Font fontName = new Font("Consolas", Font.BOLD, 14);
        public static final Font fontValue = new Font("Consolas", Font.PLAIN, 12);

        public final JLabel name = new JLabel();
        public final JLabel value = new JLabel();

        public JListEntry(String text) {
            this.name.setText(text);

            this.setLayout(new BorderLayout());
            this.add(this.name, BorderLayout.WEST);
            this.add(Box.createHorizontalStrut(100), BorderLayout.CENTER);
            this.add(this.value, BorderLayout.EAST);

            this.name.setHorizontalAlignment(JLabel.LEFT);
            this.value.setHorizontalAlignment(JLabel.RIGHT);

            this.name.setFont(fontName);
            this.value.setFont(fontValue);
        }
    }
}
