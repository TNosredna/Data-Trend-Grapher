import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.ui.RefineryUtilities
import org.jfree.data.xy.XYDataset
import org.jfree.data.time.TimeSeriesCollection
import javax.swing.JPanel
import org.jfree.chart.JFreeChart
import org.jfree.ui.ApplicationFrame
import java.awt.Color
import org.jfree.chart.plot.XYPlot
import org.jfree.ui.RectangleInsets
import org.jfree.chart.renderer.xy.XYItemRenderer
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer
import org.jfree.chart.axis.DateAxis
import java.text.SimpleDateFormat
import java.awt.BasicStroke
import org.jfree.chart.axis.ValueAxis

class TrendingChart extends ApplicationFrame {
    def config
    def dataset
    static def major_version = 1
    static def minor_version = 2

    public TrendingChart(config, parsedData) {
        super(config.get("title") + " v" + major_version + "." + minor_version);
        this.config = config
        this.dataset = createDataset(parsedData)
        ChartPanel chartPanel = (ChartPanel) createTrendingPanel();
        chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
        chartPanel.setMouseZoomable(true, false);
        chartPanel.setMouseWheelEnabled(true);
        setContentPane(chartPanel);
    }

    JFreeChart createChart(labels, XYDataset dataset, options) {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(* labels, dataset, * options)
        chart.setBackgroundPaint(Color.white);

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);

        XYItemRenderer r = plot.getRenderer();
        if (r instanceof XYLineAndShapeRenderer) {
            XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
            renderer.setBaseStroke(new BasicStroke(0.2f));
            renderer.setBaseShapesVisible(false);
            renderer.setBaseShapesFilled(false);
        }

        ValueAxis vAxis = plot.getRangeAxis();
        vAxis.setLowerBound(0);
        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat(config.get("option.timestamp-format")));
        return chart;

    }

    JPanel createTrendingPanel() {
        def labels = [config.get("label.title"), config.get("label.x-axis"), config.get("label.y-axis")]
        def options = [config.get("option.create-legend").toBoolean(), config.get("option.generate-tooltips").toBoolean(), config.get("option.generate-urls").toBoolean()]

        JFreeChart chart = createChart(labels, dataset, options);
        return new ChartPanel(chart);
    }

    XYDataset createDataset(def parsedData) {
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        parsedData.each() { key, value ->
            dataset.addSeries(value);
        }
        return dataset;
    }

    public static void main(String[] args) {
        def config = new Properties()
        new File((args != null && args.length > 0) ? args[0] : "trending.properties").withInputStream() { stream ->
            config.load(stream)
        }
        def dataParser = new TrendingChartDataParser(config);
        def parsedData = dataParser.parseDatafile()
        if (parsedData == null) {
            return
        }
        TrendingChart trendingReport = new TrendingChart(config, parsedData);
        trendingReport.pack();
        RefineryUtilities.centerFrameOnScreen(trendingReport);
        trendingReport.setVisible(true);
    }

}