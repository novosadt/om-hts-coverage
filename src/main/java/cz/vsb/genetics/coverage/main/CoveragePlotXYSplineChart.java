package cz.vsb.genetics.coverage.main;

import cz.vsb.genetics.coverage.CoverageInfo;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYShapeRenderer;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.data.xy.XYSeriesCollection;

public class CoveragePlotXYSplineChart extends CoveragePlotBase {
    protected JFreeChart createChart(String title, String xLabel, String yLabel, SamplingType samplingType, CoverageInfo... coverageInfos) {
        JFreeChart chart = ChartFactory.createXYStepChart(title, xLabel, yLabel, createDataset(coverageInfos, samplingType),
                PlotOrientation.VERTICAL, true, true, false);

        XYSplineRenderer renderer = new XYSplineRenderer();
        renderer.setFillType(XYSplineRenderer.FillType.NONE);
        chart.getXYPlot().setRenderer(renderer);

        return chart;
    }
}
