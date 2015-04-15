package com.wernerapps.solver;

import java.util.ArrayList;
import java.util.Stack;

public class ExactCover
{
    private int             lastRow;
    private Node            lastCell;
    private Node            header;
    private ArrayList<Node> columns;
    private Stack<Integer>  solution;

    public ExactCover(int numCols)
    {
        lastRow = -1;
        lastCell = null;
        header = new Node(null, null, 0, 0);
        columns = new ArrayList<Node>();
        solution = new Stack<Integer>();

        for (int i = 0; i < numCols; i++)
        {
            columns.add(new Node(header, null, 0, i));
        }
    }

    public void add(int row, int col)
    {
        // Reset if we're on a new row
        if (row != lastRow)
            lastCell = null;

        // Build our linked list
        lastCell = new Node(lastCell, columns.get(col), row, col);

        // Increment number of nodes in this column
        columns.get(col).s++;

        // Remember what row we last added to
        lastRow = row;
    }

    public void cover(int col)
    {
        Node colHead = columns.get(col);
        colHead.hideHoriz();

        // We start one below our column header, and loop until we
        // hit the header again
        for (Node colCursor = colHead.down; colCursor != colHead; colCursor = colCursor.down)
        {
            // Start at the row and go right until we come back to
            // the start
            for (Node rowCursor = colCursor.right; rowCursor != colCursor; rowCursor = rowCursor.right)
            {
                rowCursor.hideVertical();
                columns.get(rowCursor.c).s--;
            }
        }
    }

    public void uncover(int col)
    {
        Node colHead = columns.get(col);

        for (Node colCursor = colHead.up; colCursor != colHead; colCursor = colCursor.up)
        {
            for (Node rowCursor = colCursor.left; rowCursor != colCursor; rowCursor = rowCursor.left)
            {
                columns.get(rowCursor.c).s++;
                rowCursor.unhideVertical();
            }
        }

        colHead.unhideHoriz();
    }

    public boolean solve()
    {
        // Empty matrix
        if (header.right == header)
            return true;

        // Find the column with the fewest nodes
        int count = Integer.MAX_VALUE;
        Node column = null;
        for (Node cursor = header.right; cursor != header; cursor = cursor.right)
        {
            if (cursor.s < count)
            {
                column = cursor;
                count = cursor.s;
            }
        }
        // cout << "Index: " << column.c << endl;

        // Cover using our column
        cover(column.c);

        // Row is a node on a specific row, we loop down until the beginning again
        for (Node colCursor = column.down; colCursor != column; colCursor = colCursor.down)
        {
            solution.push(colCursor.s); // Add row to solution

            for (Node rowCursor = colCursor.right; rowCursor != colCursor; rowCursor = rowCursor.right)
            {
                // cout << "Cover: r = " << rowCursor.s << " c = " << rowCursor.c << endl;
                cover(rowCursor.c);
            }

            if (solve())
                return true;

            // Undo what we did
            for (Node rowCursor = colCursor.left; rowCursor != colCursor; rowCursor = rowCursor.left)
            {
                // cout << "Uncover: r = " << rowCursor.s << " c = " << rowCursor.c << endl;
                uncover(rowCursor.c);
            }

            solution.pop();
        }

        // finish undoing
        uncover(column.c);

        return false;
    }

    public Stack<Integer> getSolution()
    {
        return solution;
    }

    public void testExactCover()
    {
        String[] test = { "1001001", "1001000", "0001101", "0010110", "0110011", "0100001" };
        int NUM_ROWS = 6;

        int rows = NUM_ROWS;
        int cols = test[0].length();

        ExactCover ec = new ExactCover(cols);
        for (int i = 0; i < rows; i++)
            for (int k = 0; k < cols; k++)
                if (test[i].charAt(i) == '1')
                    ec.add(i, k);

        if (ec.solve())
        {
            Stack<Integer> sol = ec.getSolution();
            while (!sol.empty())
            {
                System.out.println(sol.peek() + " " + test[sol.peek()]);
                sol.pop();
            }
        }
    }
}
