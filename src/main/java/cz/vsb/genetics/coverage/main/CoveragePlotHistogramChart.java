package cz.vsb.genetics.coverage.main;

import cz.vsb.genetics.coverage.CoverageInfo;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;

import java.util.List;

public class CoveragePlotHistogramChart extends CoveragePlotBase {
    protected JFreeChart createChart(String title, String xLabel, String yLabel, SamplingType samplingType, List<CoverageInfo> coverageInfos) {
        return ChartFactory.createHistogram(title, xLabel, yLabel, createDataset(coverageInfos, samplingType),
                PlotOrientation.VERTICAL, true, true, false);
    }
}
