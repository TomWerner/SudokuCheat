package com.wernerapps.imageprocessing;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.ml.CvKNearest;

public class DigitIdentifier
{
    public static final int  ROWS                     = 28;
    public static final int  COLUMNS                  = 28;
    private CvKNearest       knn;
    private int              numRows = ROWS;
    private int              numCols = COLUMNS;

    public DigitIdentifier()
    {
        knn = new CvKNearest();
    }

    private class Pair
    {
        public int     label;
        public float[] data;

        public Pair(int label, float[] data)
        {
            this.label = label;
            this.data = data;
        }
    }

    public boolean train2(String trainingDataPath)
    {
        Scanner scan = null;
        try
        {
            scan = new Scanner(new File(trainingDataPath));
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        ArrayList<Pair> data = new ArrayList<Pair>();

        numRows = ROWS;
        numCols = COLUMNS;
        int size = numRows * numCols;

        while (scan.hasNextLine())
        {
            int label = scan.nextInt();
            String[] dataStr = scan.nextLine().substring(1).split(" ");
            float[] imageData = new float[dataStr.length];
            for (int i = 0; i < dataStr.length; i++)
                imageData[i] = Integer.parseInt(dataStr[i]);
            if (imageData.length > 0)
                data.add(new Pair(label, imageData));
        }

        Mat trainingVectors = new Mat();
        trainingVectors.create(9020, size, CvType.CV_32FC1);

        Mat trainingClasses = new Mat();
        trainingClasses.create(9020, 1, CvType.CV_32FC1);

        for (int i = 0; i < data.size(); i++)
        {
            Pair pair = data.get(i);
            trainingClasses.put(i, 0, pair.label);
            trainingVectors.put(i, 0, pair.data);
        }

        boolean result = knn.train(trainingVectors, trainingClasses);
        System.out.println("Trained: " + result);
        scan.close();
        return result;
    }


    public int classify(Mat img, FloodFillBounds maxBounds)
    {
        Mat cloneImg = preprocessImage(img, maxBounds);
        float value = knn.find_nearest(cloneImg, 1, new Mat(), new Mat(), new Mat());
        return (int) value;
    }

    public Mat preprocessImage(Mat img, FloodFillBounds maxBounds)
    {
        int rowTop = -1, rowBottom = -1, colLeft = -1, colRight = -1;

        rowBottom = maxBounds.maxX;
        rowTop = maxBounds.minX;
        colLeft = maxBounds.minY;
        colRight = maxBounds.maxY;

//        Core.line(img, new Point(0, rowTop), new Point(img.cols(), rowTop), new Scalar(255, 0, 0));
//        Core.line(img, new Point(0, rowBottom), new Point(img.cols(), rowBottom), new Scalar(255, 0, 0));
//        Core.line(img, new Point(colLeft, 0), new Point(colLeft, img.rows()), new Scalar(255, 0, 0));
//        Core.line(img, new Point(colRight, 0), new Point(colRight, img.rows()), new Scalar(255, 0, 0));
//        SudokuRecognizer.displayImage(SudokuRecognizer.Mat2BufferedImage(img));

        // Now, position this into the center

        Mat newImg;
        newImg = Mat.zeros(img.rows(), img.cols(), CvType.CV_8UC1);

        int startAtX = (newImg.cols() / 2) - (colRight - colLeft) / 2;
        int startAtY = (newImg.rows() / 2) - (rowBottom - rowTop) / 2;

        for (int y = startAtY; y < (newImg.rows() / 2) + (rowBottom - rowTop) / 2; y++)
        {
            byte[] row = SudokuRecognizer.getRowBytes(newImg, y);
            byte[] imgRow = SudokuRecognizer.getRowBytes(img, rowTop + (y - startAtY));
            for (int x = startAtX; x < (newImg.cols() / 2) + (colRight - colLeft) / 2; x++)
            {
                row[x] = imgRow[colLeft + (x - startAtX)];
            }
            newImg.put(y, 0, row);
        }

        Mat cloneImg = new Mat(numRows, numCols, CvType.CV_8UC1);
        Imgproc.resize(newImg, cloneImg, new Size(numCols, numRows));

        // Now fill along the borders
        for (int i = 0; i < cloneImg.rows(); i++)
        {
            Imgproc.floodFill(cloneImg, SudokuRecognizer.createFloodMask(cloneImg), new Point(0, i),
                    new Scalar(0, 0, 0));
            Imgproc.floodFill(cloneImg, SudokuRecognizer.createFloodMask(cloneImg), new Point(cloneImg.cols() - 1, i),
                    new Scalar(0, 0, 0));

            Imgproc.floodFill(cloneImg, SudokuRecognizer.createFloodMask(cloneImg), new Point(i, 0), new Scalar(0));
            Imgproc.floodFill(cloneImg, SudokuRecognizer.createFloodMask(cloneImg), new Point(i, cloneImg.rows() - 1),
                    new Scalar(0));
        }

//        SudokuRecognizer.displayImage(SudokuRecognizer.Mat2BufferedImage(cloneImg));
        
        cloneImg = cloneImg.reshape(1, 1);
        cloneImg.convertTo(cloneImg, CvType.CV_32F);
        return cloneImg;
    }
}
