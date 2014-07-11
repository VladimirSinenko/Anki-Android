/****************************************************************************************
 * Copyright (c) 2011 Norbert Nagold <norbert.nagold@gmail.com>                         *
 * Copyright (c) 2014 Michael Goldbach <trashcutter@googlemail.com>                     *
 *                                                                                      *
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 *                                                                                      *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 *                                                                                      *
 * You should have received a copy of the GNU General Public License along with         *
 * this program.  If not, see <http://www.gnu.org/licenses/>.                           *
 ****************************************************************************************/
package com.ichi2.anki.stats;

import android.graphics.Bitmap;
import android.widget.ImageView;
import com.ichi2.anki.R;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Stats;
import com.wildplot.android.rendering.*;
import com.wildplot.android.rendering.graphics.wrapper.BufferedImage;
import com.wildplot.android.rendering.graphics.wrapper.Color;
import com.wildplot.android.rendering.graphics.wrapper.Graphics2D;
import com.wildplot.android.rendering.graphics.wrapper.Rectangle;

/**
 * Created by mig on 06.07.2014.
 */
public class AnswerButton {
    private final boolean mIsWholeCollection;
    private ImageView mImageView;
    private Collection mCollectionData;

    private int mFrameThickness = 60;
    private int targetPixelDistanceBetweenTics = 150;


    private int mMaxCards = 0;
    private int mMaxElements = 0;
    private int[] mValueLabels;
    private int[] mColors;
    private int[] mAxisTitles;
    private double[][] mSeriesList;
    private double mBarThickness = 0.8;
    private double[][] mCumulative;


    public AnswerButton(ImageView imageView, Collection collectionData, boolean isWholeCollection){
        mImageView = imageView;
        mCollectionData = collectionData;
        mIsWholeCollection = isWholeCollection;
    }
    private void calcStats(int type){
        Stats stats = new Stats(mCollectionData, mIsWholeCollection);
        stats.calculateAnswerButtons(type);
        mCumulative = stats.getCumulative();
        mSeriesList = stats.getSeriesList();
        Object[] metaData = stats.getMetaInfo();
        mValueLabels = (int[]) metaData[3];
        mColors = (int[]) metaData[4];
        mAxisTitles = (int[]) metaData[5];
        mMaxCards = (Integer) metaData[7];
        mMaxElements = (Integer)metaData[8];
    }

    public Bitmap renderChart(int type) {
        calcStats(type);
        int height = mImageView.getMeasuredHeight();
        int width = mImageView.getMeasuredWidth();

        if(height <=0 || width <= 0){
            return null;
        }

        BufferedImage bufferedFrameImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bufferedFrameImage.createGraphics();
        Rectangle rect = new Rectangle(width, height);
        g.setClip(rect);
        g.setColor(Color.BLACK);
        float textSize = AnkiStatsTaskHandler.getInstance().getmStandardTextSize()*0.75f;
        g.setFontSize(textSize);

        float FontHeigth = g.getFontMetrics().getHeight(true);
        mFrameThickness = Math.round( FontHeigth * 4.0f);
        //System.out.println("frame thickness: " + mFrameThickness);

        PlotSheet plotSheet = new PlotSheet(0, 15, 0, mMaxCards*1.03);
        plotSheet.setFrameThickness(mFrameThickness);

        //no title because of tab title
        //plotSheet.setTitle(mImageView.getResources().getString(mTitle));

        double xTics = ticsCalcX(targetPixelDistanceBetweenTics, rect);
        double yTics = ticsCalcY(targetPixelDistanceBetweenTics, rect);

        XAxis xaxis = new XAxis(plotSheet, 0, xTics, xTics/2.0);
        YAxis yaxis = new YAxis(plotSheet, 0, yTics, yTics/2.0);
        double[] timePositions = {1,2,3,6,7,8,9,11,12,13,14};
        xaxis.setExplicitTics(timePositions, mImageView.getResources().getStringArray(R.array.stats_eases_ticks));
        xaxis.setOnFrame();
        xaxis.setName(mImageView.getResources().getString(mAxisTitles[0]));
        xaxis.setIntegerNumbering(true);
        yaxis.setIntegerNumbering(true);
        yaxis.setName(mImageView.getResources().getString(mAxisTitles[1]));
        yaxis.setOnFrame();



        BarGraph[] barGraphs = new BarGraph[mSeriesList.length-1];
        for(int i = 1; i< mSeriesList.length; i++){
            double[][] bars = new double[2][];
            bars[0] = mSeriesList[0];
            bars[1] = mSeriesList[i];

            barGraphs[i-1] = new BarGraph(plotSheet, mBarThickness, bars, new Color(mImageView.getResources().getColor(mColors[i-1])));
            barGraphs[i-1].setFilling(true);
            barGraphs[i-1].setName(mImageView.getResources().getString(mValueLabels[i-1]));
            //barGraph.setFillColor(Color.GREEN.darker());
            barGraphs[i-1].setFillColor(new Color(mImageView.getResources().getColor(mColors[i-1])));
        }

        PlotSheet hiddenPlotSheet = new PlotSheet(0, 15, 0, 101);     //for second y-axis
        hiddenPlotSheet.setFrameThickness(mFrameThickness);

        Lines[] lineses = new Lines[mCumulative.length - 1];
        for(int i = 1; i< mCumulative.length; i++){
            double[][] cumulatives = new double[][]{mCumulative[0], mCumulative[i]};
            lineses[i-1] = new Lines(hiddenPlotSheet,cumulatives ,new Color(mImageView.getResources().getColor(mColors[i-1])));
            lineses[i-1].setSize(3f);
            lineses[i-1].setShadow(5f, 3f, 3f, Color.BLACK);
            //No names to prevent double entries in legend:
            //lineses[i-1].setName(mImageView.getResources().getString(R.string.stats_cumulative));
        }

        double rightYtics = ticsCalc(targetPixelDistanceBetweenTics, rect,  101);
        YAxis rightYaxis = new YAxis(hiddenPlotSheet, 0, rightYtics, rightYtics/2.0);
        rightYaxis.setIntegerNumbering(true);
        rightYaxis.setName(mImageView.getResources().getString(mAxisTitles[2]));
        rightYaxis.setOnRightSideFrame();

        int red = Color.LIGHT_GRAY.getRed();
        int green = Color.LIGHT_GRAY.getGreen();
        int blue = Color.LIGHT_GRAY.getBlue();

        Color newGridColor = new Color(red,green,blue, 222);

        XGrid xGrid = new XGrid(plotSheet, 0, targetPixelDistanceBetweenTics);
        YGrid yGrid = new YGrid(plotSheet, 0, targetPixelDistanceBetweenTics);

        xGrid.setColor(newGridColor);
        yGrid.setColor(newGridColor);
        yGrid.setExplicitTics(timePositions);
        plotSheet.setFontSize(textSize);

        for(BarGraph barGraph : barGraphs){
            plotSheet.addDrawable(barGraph);
        }

        for(Lines lines : lineses){
            plotSheet.addDrawable(lines);
        }
        plotSheet.addDrawable(xaxis);
        plotSheet.addDrawable(yaxis);
        plotSheet.addDrawable(rightYaxis);
        plotSheet.addDrawable(xGrid);
        plotSheet.addDrawable(yGrid);
        plotSheet.paint(g);
        Bitmap bitmap = bufferedFrameImage.getBitmap();
        bitmap.prepareToDraw();
        return bitmap;
    }



    public double ticsCalcX(int pixelDistance, Rectangle field){
        double deltaRange =mMaxElements - 0;
        int ticlimit = field.width/pixelDistance;
        double tics = Math.pow(10, (int)Math.log10(deltaRange/ticlimit));
        while(2.0*(deltaRange/(tics)) <= ticlimit) {
            tics /= 2.0;
        }
        while((deltaRange/(tics))/2 >= ticlimit) {
            tics *= 2.0;
        }
        return tics;
    }

    public double ticsCalcY(int pixelDistance, Rectangle field){
        double deltaRange = mMaxCards - 0;
        int ticlimit = field.height/pixelDistance;
        double tics = Math.pow(10, (int)Math.log10(deltaRange/ticlimit));
        while(2.0*(deltaRange/(tics)) <= ticlimit) {
            tics /= 2.0;
        }
        while((deltaRange/(tics))/2 >= ticlimit) {
            tics *= 2.0;
        }
        return tics;
    }

    public double ticsCalc(int pixelDistance, Rectangle field, double deltaRange){
        int ticlimit = field.height/pixelDistance;
        double tics = Math.pow(10, (int)Math.log10(deltaRange/ticlimit));
        while(2.0*(deltaRange/(tics)) <= ticlimit) {
            tics /= 2.0;
        }
        while((deltaRange/(tics))/2 >= ticlimit) {
            tics *= 2.0;
        }
        return tics;
    }




}
