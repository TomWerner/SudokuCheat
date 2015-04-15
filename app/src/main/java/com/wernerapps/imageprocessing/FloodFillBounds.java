package com.wernerapps.imageprocessing;

public class FloodFillBounds
{
    public int area;
    public int minX;
    public int minY;
    public int maxX;
    public int maxY;
    
    public FloodFillBounds(int area, int minX, int minY, int maxX, int maxY)
    {
        this.area = area;
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }
    
    public String toString()
    {
        return "Area: " + area + ", " + minX + " , " + maxX + " , " + minY + " , " + maxY;
    }
    
}
