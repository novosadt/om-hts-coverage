/*
 * Copyright (C) 2025  Tomas Novosad
 * VSB-TUO, Faculty of Electrical Engineering and Computer Science
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


package cz.vsb.genetics.coverage.main;

import cz.vsb.genetics.coverage.CoverageInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public abstract class CoveragePlotBase implements CoveragePlot {
    private static final Logger log = LoggerFactory.getLogger(CoveragePlot.class);

    protected int width = 1600;
    protected int height = 1200;

    protected abstract JFreeChart createChart(String title, String xLabel, String yLabel, SamplingType samplingType, List<CoverageInfo> coverageInfos);

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public void plotCoverage(String title, String xLabel, String yLabel, String outputFile, SamplingType samplingType, List<CoverageInfo> coverageInfos, ImageFormat format) throws Exception {
        NumberAxis domainAxis = new NumberAxis(xLabel);
        setupDomainAxisRange(domainAxis, coverageInfos);

        JFreeChart coverageChart = createChart(title, xLabel, yLabel, samplingType, coverageInfos);

        XYPlot xyPlot = coverageChart.getXYPlot();
        xyPlot.setDomainAxis(domainAxis);
        xyPlot.setForegroundAlpha(0.85F);

        int coverageLimit = getCoverageLimit(coverageInfos);
        if (coverageLimit > 0) {
            NumberAxis rangeAxis = new NumberAxis(yLabel);
            rangeAxis.setRange(0, coverageLimit);
            rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
            xyPlot.setRangeAxis(rangeAxis);
        }

        setupSerieColors(xyPlot, coverageInfos);

        Font fontLabel = new Font("Dialog", Font.PLAIN, 25);
        Font fontTick = new Font("Dialog", Font.PLAIN, 15);
        Font fontLegend = new Font("Dialog", Font.PLAIN, 15);
        xyPlot.getDomainAxis().setLabelFont(fontLabel);
        xyPlot.getRangeAxis().setLabelFont(fontLabel);
        xyPlot.getDomainAxis().setTickLabelFont(fontTick);
        xyPlot.getRangeAxis().setTickLabelFont(fontTick);

        coverageChart.getLegend().setItemFont(fontLegend);

        log.info(String.format("Plotting image (format %s): %s", format.toString(), outputFile));
        ImageWriter.saveImage(outputFile, coverageChart, width, height, format);
    }

    protected XYSeriesCollection createDataset(List<CoverageInfo> coverageInfos, SamplingType samplingType) {
        XYSeriesCollection dataset = new XYSeriesCollection();

        for (CoverageInfo coverageInfo : coverageInfos) {
            if (coverageInfo == null)
                continue;

            XYSeries series = new XYSeries(coverageInfo.getName());
            int[] coverage = coverageInfo.getCoverages();

            if (coverageInfo.getSamplingSize() < 3)
                samplingType = SamplingType.NONE;

            switch (samplingType) {
                case NONE: sampleNone(coverageInfo, series, coverage); break;
                case MEAN: sampleMean(coverageInfo, series, coverage); break;
                case MEDIAN: sampleMedian(coverageInfo, series, coverage); break;
                default: sampleRandom(coverageInfo, series, coverage); break;
            }

            dataset.addSeries(series);
        }

        return dataset;
    }

    private static void sampleNone(CoverageInfo coverageInfo, XYSeries series, int[] coverage) {
        for (int i = 0, j = coverageInfo.getPositionStart(); i < coverage.length; i++, j++)
            series.add(j, coverage[i]);
    }

    private static void sampleRandom(CoverageInfo coverageInfo, XYSeries series, int[] coverage) {
        int sampleSize = coverageInfo.getSamplingSize() - 1;
        Random random = new Random();

        for (int i = 0, j = coverageInfo.getPositionStart(); i < coverage.length; i += sampleSize, j += sampleSize) {
            int index = random.nextInt(sampleSize) + i;

            if (index >= coverage.length) {
                index = coverage.length - 1;
                j = coverageInfo.getPositionEnd();
            }

            series.add(j, coverage[index]);
        }
    }

    private static void sampleMean(CoverageInfo coverageInfo, XYSeries series, int[] coverage) {
        int sampleSize = coverageInfo.getSamplingSize() - 1;
        int counter = 0;
        int sum = 0;

        for (int i = 0; i < coverage.length; i++) {
            sum += coverage[i];

            if (++counter == sampleSize) {
                series.add(coverageInfo.getPositionStart() + (i - sampleSize / 2) , sum / sampleSize);
                sum = 0;
                counter = 0;
            }
        }

        if (counter > 0)
            series.add(coverageInfo.getPositionEnd() - counter / 2 , sum / counter);
    }

    private static void sampleMedian(CoverageInfo coverageInfo, XYSeries series, int[] coverages) {
        int sampleSize = coverageInfo.getSamplingSize();

        List<Integer> values = new ArrayList<>();

        for (int i = 0; i < coverages.length; i++) {
            values.add(coverages[i]);

            if (values.size() == sampleSize) {
                Collections.sort(values);
                series.add(coverageInfo.getPositionStart() + (i - sampleSize / 2) , values.get((values.size() -1) / 2));
                values.clear();
            }
        }

        if (!values.isEmpty()) {
            Collections.sort(values);
            series.add(coverageInfo.getPositionEnd() - values.size() / 2, values.get((values.size() -1) / 2));
        }
    }

    protected int getCoverageLimit(List<CoverageInfo> coverageInfos) {
        int max = 0;

        for (CoverageInfo coverageInfo : coverageInfos)
            if (coverageInfo.getCoverageLimit() > max)
                max = coverageInfo.getCoverageLimit();

        return max;
    }

    private void setupDomainAxisRange(NumberAxis domainAxis, List<CoverageInfo> coverageInfos) {
        int lower = Integer.MAX_VALUE;
        int upper = 0;

        for (CoverageInfo coverageInfo : coverageInfos) {
            if (coverageInfo.getPositionStart() < lower)
                lower = coverageInfo.getPositionStart();

            if (coverageInfo.getPositionEnd() > upper)
                upper = coverageInfo.getPositionEnd();
        }

        domainAxis.setRange(lower, upper);
    }

    private void setupSerieColors(XYPlot xyPlot, List<CoverageInfo> coverageInfos) {
        for (CoverageInfo coverageInfo : coverageInfos)
            if (coverageInfo.getColor() == null)
                return;

        Paint[] colors = new Paint[coverageInfos.size()];
        for (int i = 0; i < colors.length; i++)
            colors[i] = new Color(coverageInfos.get(i).getColor(), true);

        xyPlot.setDrawingSupplier(new DefaultDrawingSupplier(
                DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_FILL_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));
    }
}
