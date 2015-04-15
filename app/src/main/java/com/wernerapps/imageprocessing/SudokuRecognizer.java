package com.wernerapps.imageprocessing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Stack;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

public class SudokuRecognizer
{
    static{ System.loadLibrary("opencv_java"); }

    public static String recognizeSudoku(String filename)
    {
//        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        Mat sudoku = Highgui.imread(filename, Highgui.IMREAD_GRAYSCALE);

        Mat outerBox = preprocessGrid(sudoku);

        Mat lines = new Mat();
        // Run a line detection algorithm to find the outer lines of the puzzle
        Imgproc.HoughLines(outerBox, lines, 1, Math.PI / 180, 200);
        // Merge the related lines so we have fewer
        mergeRelatedLines(lines, sudoku); // Add this line


        // Now detect the lines on extremes
        double[] topEdge = { 1000, 1000 };
        double[] bottomEdge = { -1000, -1000 };
        double[] leftEdge = { 1000, 1000 };
        double leftXIntercept = 100000;
        double[] rightEdge = { -1000, -1000 };
        double rightXIntercept = 0;
        Point left1 = new Point();
        Point left2 = new Point();
        Point right1 = new Point();
        Point right2 = new Point();
        Point bottom1 = new Point();
        Point bottom2 = new Point();
        Point top1 = new Point();
        Point top2 = new Point();
        findOutermostLines(outerBox, lines, topEdge, bottomEdge, leftEdge, leftXIntercept, rightEdge, rightXIntercept,
                left1, left2, right1, right2, bottom1, bottom2, top1, top2);

        // Now find the intersection of those lines
        Point2D ptTopLeft = new Point2D.Double();
        Point2D ptTopRight = new Point2D.Double();
        Point2D ptBottomRight = new Point2D.Double();
        Point2D ptBottomLeft = new Point2D.Double();
        int maxLength = findOuterLineIntersections(left1, left2, right1, right2, bottom1, bottom2, top1, top2,
                ptTopLeft, ptTopRight, ptBottomRight, ptBottomLeft);

        // Use those four points to "unwarp" the image so its decently flat
        // and the only thing in the image. This is in the original colors of the image
        Mat undistorted = new Mat(new Size(maxLength, maxLength), CvType.CV_8UC1);
        transformToSudokuSquare(sudoku, ptTopLeft, ptTopRight, ptBottomRight, ptBottomLeft, maxLength, undistorted);

        // Transform the image into black and white so numbers are white on a black background
        Mat undistortedThreshed = undistorted.clone();
        Imgproc.adaptiveThreshold(undistorted, undistortedThreshed, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY_INV, 11, 1);
        
        // For debugging purposes
//        displayImage(Mat2BufferedImage(undistortedThreshed));
        
        // Train our digit identifier
        DigitIdentifier digitIdentifier = new DigitIdentifier();
//        digitIdentifier.train("t10k-images.idx3-ubyte", "t10k-labels.idx1-ubyte");
        digitIdentifier.train2("training_data.dat");

        return identifyCellsAndDigits(maxLength, undistortedThreshed, digitIdentifier);
    }

    private static String identifyCellsAndDigits(int maxLength, Mat undistortedThreshed, DigitIdentifier digitIdentifier)
    {
        String result = "";
        int dist = (int) Math.ceil((double) maxLength / 9);
        Mat currentCell = new Mat(dist, dist, CvType.CV_8UC1);
        for (int j = 0; j < 9; j++)
        {
            for (int i = 0; i < 9; i++)
            {
                // We copy the current cell into a separate image
                // undistortedThreshed.adjustROI(i*dist, i*dist+dist, j*dist, j*dist+dist);
                for (int y = 0; y < dist && j * dist + y < undistortedThreshed.cols(); y++)
                {
                    byte[] row = getRowBytes(currentCell, y);
                    int[] undistRow = getRow(undistortedThreshed, j * dist + y);
                    // uchar* ptr2 = &(undistortedThreshed.ptr<uchar>(i*dist)[j*dist]);
                    for (int x = 0; x < dist && i * dist + x < undistortedThreshed.rows(); x++)
                    {
                        row[x] = (byte) undistRow[i * dist + x]; // undistortedThreshed.at<uchar>(j*dist+y, i*dist+x);
                    }
                    currentCell.put(y, 0, row);
                }

                // Find the largest white area in the cell that starts inside the middle
                // third. This is most likely to be our number.
                // +---+---+---+
                // |   |   |   |
                // +---+---+---+
                // |   | X |   |
                // +---+---+---+
                // |   |   |   |
                // +---+---+---+
                // If we divide the cell into 9 pieces, the "number" must start
                // inside the X marked area.
                int maxArea = -1;
                FloodFillBounds maxBounds = null;
                for (int y = currentCell.cols() / 3; y < currentCell.cols() * 2 / 3; y++)
                {
                    for (int x = currentCell.rows() / 3; x < currentCell.rows() * 2 / 3; x++)
                    {
                        FloodFillBounds fill = floodFill(currentCell, new Point(x, y), -1);
                        if (fill.area > maxArea)
                        {
                            maxArea = fill.area;
                            maxBounds = fill;
                        }
                    }
                }
                
                if (maxBounds.area > 100)
                {
                    int number = digitIdentifier.classify(currentCell, maxBounds);
                    result += number;
                }
                else
                {
                    result += " ";
                }

            }
        }
        return result;
    }

    private static void transformToSudokuSquare(Mat sudoku, Point2D ptTopLeft, Point2D ptTopRight,
            Point2D ptBottomRight, Point2D ptBottomLeft, int maxLength, Mat undistorted)
    {
        Point2D[] src = new Point2D[4];
        Point2D[] dst = new Point2D[4];
        src[0] = ptTopLeft;
        dst[0] = new Point2D.Double(0, 0);
        src[1] = ptTopRight;
        dst[1] = new Point2D.Double(maxLength - 1, 0);
        src[2] = ptBottomRight;
        dst[2] = new Point2D.Double(maxLength - 1, maxLength - 1);
        src[3] = ptBottomLeft;
        dst[3] = new Point2D.Double(0, maxLength - 1);

        Imgproc.warpPerspective(sudoku, undistorted,
                Imgproc.getPerspectiveTransform(point2dToMat(src), point2dToMat(dst)), new Size(maxLength, maxLength));
    }

    private static int findOuterLineIntersections(Point left1, Point left2, Point right1, Point right2, Point bottom1,
            Point bottom2, Point top1, Point top2, Point2D ptTopLeft, Point2D ptTopRight, Point2D ptBottomRight,
            Point2D ptBottomLeft)
    {
        // Next, we find the intersection of these four lines
        double leftA = left2.y - left1.y;
        double leftB = left1.x - left2.x;

        double leftC = leftA * left1.x + leftB * left1.y;

        double rightA = right2.y - right1.y;
        double rightB = right1.x - right2.x;

        double rightC = rightA * right1.x + rightB * right1.y;

        double topA = top2.y - top1.y;
        double topB = top1.x - top2.x;

        double topC = topA * top1.x + topB * top1.y;

        double bottomA = bottom2.y - bottom1.y;
        double bottomB = bottom1.x - bottom2.x;

        double bottomC = bottomA * bottom1.x + bottomB * bottom1.y;

        // Intersection of left and top
        double detTopLeft = leftA * topB - leftB * topA;

        ptTopLeft.setLocation((topB * leftC - leftB * topC) / detTopLeft, (leftA * topC - topA * leftC) / detTopLeft);

        // Intersection of top and right
        double detTopRight = rightA * topB - rightB * topA;

        ptTopRight.setLocation((topB * rightC - rightB * topC) / detTopRight, (rightA * topC - topA * rightC)
                / detTopRight);

        // Intersection of right and bottom
        double detBottomRight = rightA * bottomB - rightB * bottomA;
        ptBottomRight.setLocation((bottomB * rightC - rightB * bottomC) / detBottomRight, (rightA * bottomC - bottomA
                * rightC)
                / detBottomRight);// Intersection of bottom and left
        double detBottomLeft = leftA * bottomB - leftB * bottomA;
        ptBottomLeft.setLocation((bottomB * leftC - leftB * bottomC) / detBottomLeft, (leftA * bottomC - bottomA
                * leftC)
                / detBottomLeft);

        int maxLength = (int) ((ptBottomLeft.getX() - ptBottomRight.getX())
                * (ptBottomLeft.getX() - ptBottomRight.getX()) + (ptBottomLeft.getY() - ptBottomRight.getY())
                * (ptBottomLeft.getY() - ptBottomRight.getY()));
        int temp = (int) ((ptTopRight.getX() - ptBottomRight.getX()) * (ptTopRight.getX() - ptBottomRight.getX()) + (ptTopRight
                .getY() - ptBottomRight.getY()) * (ptTopRight.getY() - ptBottomRight.getY()));

        if (temp > maxLength)
            maxLength = temp;

        temp = (int) ((ptTopRight.getX() - ptTopLeft.getX()) * (ptTopRight.getX() - ptTopLeft.getX()) + (ptTopRight
                .getY() - ptTopLeft.getY()) * (ptTopRight.getY() - ptTopLeft.getY()));

        if (temp > maxLength)
            maxLength = temp;

        temp = (int) ((ptBottomLeft.getX() - ptTopLeft.getX()) * (ptBottomLeft.getX() - ptTopLeft.getX()) + (ptBottomLeft
                .getY() - ptTopLeft.getY()) * (ptBottomLeft.getY() - ptTopLeft.getY()));

        if (temp > maxLength)
            maxLength = temp;

        maxLength = (int) Math.sqrt((double) maxLength);
        return maxLength;
    }

    private static void findOutermostLines(Mat outerBox, Mat lines, double[] topEdge, double[] bottomEdge,
            double[] leftEdge, double leftXIntercept, double[] rightEdge, double rightXIntercept, Point left1,
            Point left2, Point right1, Point right2, Point bottom1, Point bottom2, Point top1, Point top2)
    {
        for (int i = 0; i < lines.cols(); i++)
        {
            double[] current = getLine(lines, i);

            float p = (float) current[0];
            float theta = (float) current[1];

            if (p == 0 && theta == -100)
                continue;

            double xIntercept = p / Math.cos(theta);
            int wiggleRoom = 10; // Degrees off vertical or horizontal

            if (theta > Math.PI * (90 - wiggleRoom) / 180 && theta < Math.PI * (90 + wiggleRoom) / 180)
            {
                if (p < topEdge[0] && p > 0)
                    topEdge = current;

                if (p > bottomEdge[0])
                    bottomEdge = current;
            }
            else if (theta < Math.PI * wiggleRoom / 180 || theta > Math.PI * (180 - wiggleRoom) / 180)
            {
                if (xIntercept > rightXIntercept)
                {
                    rightEdge = current;
                    rightXIntercept = xIntercept;
                }
                else if (xIntercept <= leftXIntercept)
                {
                    leftEdge = current;
                    leftXIntercept = xIntercept;
                }
            }
        }

        int height = (int) outerBox.size().height;

        int width = (int) outerBox.size().width;

        if (leftEdge[1] != 0)
        {
            left1.x = 0;
            left1.y = leftEdge[0] / Math.sin(leftEdge[1]);
            left2.x = width;
            left2.y = -left2.x / Math.tan(leftEdge[1]) + left1.y;
        }
        else
        {
            left1.y = 0;
            left1.x = leftEdge[0] / Math.cos(leftEdge[1]);
            left2.y = height;
            left2.x = left1.x - height * Math.tan(leftEdge[1]);

        }

        if (rightEdge[1] != 0)
        {
            right1.x = 0;
            right1.y = rightEdge[0] / Math.sin(rightEdge[1]);
            right2.x = width;
            right2.y = -right2.x / Math.tan(rightEdge[1]) + right1.y;
        }
        else
        {
            right1.y = 0;
            right1.x = rightEdge[0] / Math.cos(rightEdge[1]);
            right2.y = height;
            right2.x = right1.x - height * Math.tan(rightEdge[1]);

        }

        bottom1.x = 0;
        bottom1.y = bottomEdge[0] / Math.sin(bottomEdge[1]);

        bottom2.x = width;
        bottom2.y = -bottom2.x / Math.tan(bottomEdge[1]) + bottom1.y;

        top1.x = 0;
        top1.y = topEdge[0] / Math.sin(topEdge[1]);
        top2.x = width;
        top2.y = -top2.x / Math.tan(topEdge[1]) + top1.y;
    }

    private static Mat preprocessGrid(Mat sudoku)
    {
        Mat outerBox = new Mat(sudoku.size(), CvType.CV_8UC1);

        // Remove imperfections, threshold it, and reverse the colors, so the lines are white on a black background
        Imgproc.GaussianBlur(sudoku, sudoku, new Size(11, 11), 0);
        Imgproc.adaptiveThreshold(sudoku, outerBox, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 7, 2);
        Core.bitwise_not(outerBox, outerBox);

        Mat kernel = Mat.zeros(3, 3, CvType.CV_8U);
        kernel.put(0, 0, new byte[] { 0, 1, 0 });
        kernel.put(0, 0, new byte[] { 1, 1, 1 });
        kernel.put(0, 0, new byte[] { 0, 1, 0 });

        // Dialate the image to ensure that lines are connected
        Imgproc.dilate(outerBox, outerBox, kernel);
        
        // Find the largest continuous white area by repeatedly floodfilling
        int max = -1;

        Point maxPt = null;
        for (int y = 0; y < outerBox.size().height; y++)
        {
            int[] row = getRow(outerBox, y);
            for (int x = 0; x < outerBox.size().width; x++)
            {
                if (row[x] >= 180)
                {
                    int area = Imgproc.floodFill(outerBox, createFloodMask(outerBox), new Point(x, y), new Scalar(60,
                            60, 60));

                    if (area > max)
                    {
                        maxPt = new Point(x, y);
                        max = area;
                    }
                }
            }
        }

        // Flood fill the largest white area completely white
        Imgproc.floodFill(outerBox, createFloodMask(outerBox), maxPt, new Scalar(255, 255, 255));

        // Make everything else black.
        for (int y = 0; y < outerBox.size().height; y++)
        {
            int[] row = getRow(outerBox, y);
            for (int x = 0; x < outerBox.size().width; x++)
                if (row[x] == 60 && x != maxPt.x && y != maxPt.y)
                    Imgproc.floodFill(outerBox, createFloodMask(outerBox), new Point(x, y), new Scalar(0, 0, 0));
        }

        // Undo the dialate
        Imgproc.erode(outerBox, outerBox, kernel);
        return outerBox;
    }

    private static FloodFillBounds floodFill(Mat img, Point point, int value)
    {
        Stack<Point> pointsLeft = new Stack<Point>();
        pointsLeft.push(point);
        int minX = (int) point.x;
        int maxX = (int) point.x;
        int minY = (int) point.y;
        int maxY = (int) point.y;
        int area = 0;

        byte[] data = new byte[img.rows() * img.cols()];
        img.get(0, 0, data);
        HashSet<Point> done = new HashSet<Point>();

        while (!pointsLeft.isEmpty())
        {
            Point test = pointsLeft.pop();
            done.add(test);
            if (data[((int) test.x) * img.rows() + ((int) test.y)] == value)
            {
                area += 1;
                minX = (int) Math.min(minX, test.x);
                maxX = (int) Math.max(maxX, test.x);
                minY = (int) Math.min(minY, test.y);
                maxY = (int) Math.max(maxY, test.y);
                for (Point neighbor : getNeighbors(img.rows(), img.cols(), (int) test.x, (int) test.y))
                {
                    if (!done.contains(neighbor))
                        pointsLeft.push(neighbor);
                }
            }

        }

        return new FloodFillBounds(area, minX, minY, maxX, maxY);
    }

    private static ArrayList<Point> getNeighbors(int rows, int cols, int x, int y)
    {
        ArrayList<Point> result = new ArrayList<Point>();

        if (x > 0)
            result.add(new Point(x - 1, y));
        if (x < rows - 1)
            result.add(new Point(x + 1, y));
        if (y > 0)
            result.add(new Point(x, y - 1));
        if (y < cols - 1)
            result.add(new Point(x, y + 1));

        return result;
    }

    private static Mat point2dToMat(Point2D[] src)
    {
        Mat mat = new Mat(src.length, 1, CvType.CV_32FC2);
        for (int i = 0; i < src.length; i++)
            setLine(mat, i, new double[] { src[i].getX(), src[i].getY() });
        return mat;
    }

    public static void drawLine(double[] line, Mat img, Scalar rgb)
    {
        if (line[1] != 0)
        {
            float m = (float) (-1 / Math.tan(line[1]));

            float c = (float) (line[0] / Math.sin(line[1]));

            Core.line(img, new Point(0, c), new Point(img.size().width, m * img.size().width + c), rgb);
        }
        else
        {
            Core.line(img, new Point(line[0], 0), new Point(line[0], img.size().height), rgb);
        }

    }

    public static Mat createFloodMask(Mat outerBox)
    {
        return new Mat(outerBox.height() + 2, outerBox.width() + 2, outerBox.type());
    }

    public static int[] getRow(Mat mat, int y)
    {
        String row = mat.row(y).dump();
        row = row.substring(1, row.length() - 1);
        String[] elems = row.split(", ");
        int[] result = new int[elems.length];
        for (int i = 0; i < elems.length; i++)
            result[i] = Integer.parseInt(elems[i]);
        return result;
    }

    public static byte[] getRowBytes(Mat mat, int y)
    {
        String row = mat.row(y).dump();
        row = row.substring(1, row.length() - 1);
        String[] elems = row.split(", ");
        byte[] result = new byte[elems.length];
        for (int i = 0; i < elems.length; i++)
            result[i] = (byte) Integer.parseInt(elems[i]);
        return result;
    }

    private static void mergeRelatedLines(Mat lines, Mat img)
    {
        for (int i = 0; i < lines.cols(); i++)
        {
            double[] current = getLine(lines, i);
            if (current[0] == 0 && current[1] == -100)
                continue;
            float p1 = (float) current[0];
            float theta1 = (float) current[1];

            Point pt1current = new Point();
            Point pt2current = new Point();
            if (theta1 > Math.PI * 45 / 180 && theta1 < Math.PI * 135 / 180)
            {
                pt1current.x = 0;

                pt1current.y = p1 / Math.sin(theta1);

                pt2current.x = img.size().width;
                pt2current.y = -pt2current.x / Math.tan(theta1) + p1 / Math.sin(theta1);
            }
            else
            {
                pt1current.y = 0;

                pt1current.x = p1 / Math.cos(theta1);

                pt2current.y = img.size().height;
                pt2current.x = -pt2current.y / Math.tan(theta1) + p1 / Math.cos(theta1);
            }

            for (int k = 0; k < lines.cols(); k++)
            {
                if (i == k)
                    continue;
                double[] pos = getLine(lines, k);

                if (Math.abs(pos[0] - current[0]) < 20 && Math.abs((pos)[1] - (current)[1]) < Math.PI * 10 / 180)
                {
                    float p = (float) (pos)[0];
                    float theta = (float) (pos)[1];

                    Point pt1 = new Point();
                    Point pt2 = new Point();
                    if ((pos)[1] > Math.PI * 45 / 180 && (pos)[1] < Math.PI * 135 / 180)
                    {
                        pt1.x = 0;
                        pt1.y = p / Math.sin(theta);
                        pt2.x = img.size().width;
                        pt2.y = -pt2.x / Math.tan(theta) + p / Math.sin(theta);
                    }
                    else
                    {
                        pt1.y = 0;
                        pt1.x = p / Math.cos(theta);
                        pt2.y = img.size().height;
                        pt2.x = -pt2.y / Math.tan(theta) + p / Math.cos(theta);
                    }

                    if (((double) (pt1.x - pt1current.x) * (pt1.x - pt1current.x) + (pt1.y - pt1current.y)
                            * (pt1.y - pt1current.y) < 64 * 64)
                            && ((double) (pt2.x - pt2current.x) * (pt2.x - pt2current.x) + (pt2.y - pt2current.y)
                                    * (pt2.y - pt2current.y) < 64 * 64))
                    {
                        // Merge the two
                        (current)[0] = ((current)[0] + (pos)[0]) / 2;

                        (current)[1] = ((current)[1] + (pos)[1]) / 2;

                        (pos)[0] = 0;
                        (pos)[1] = -100;

                    }
                }
            }
        }
    }

    public static double[] getLine(Mat mat, int y)
    {
        String row = mat.col(y).dump();
        row = row.substring(1, row.length() - 1);
        String[] elems = row.split(", ");
        double[] result = new double[elems.length];
        for (int i = 0; i < elems.length; i++)
            result[i] = Double.parseDouble(elems[i]);
        return result;
    }

    public static void setLine(Mat mat, int y, double[] newLine)
    {
        mat.row(y).put(0, 0, newLine);
    }
}